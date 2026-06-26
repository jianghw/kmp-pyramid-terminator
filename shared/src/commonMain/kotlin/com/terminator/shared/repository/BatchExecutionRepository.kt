package com.terminator.shared.repository

import com.terminator.shared.model.BatchExecution
import com.terminator.shared.model.BatchExecutionConfig
import com.terminator.shared.model.BatchProgress
import com.terminator.shared.model.BatchStatus
import com.terminator.shared.model.BatchTaskResult
import com.terminator.shared.model.ExecutionStatus
import com.terminator.shared.model.TaskExecution
import com.terminator.shared.model.TaskTemplate
import com.terminator.shared.network.api.TaskApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

class BatchExecutionRepository(
    private val taskApi: TaskApi,
    private val taskRepository: TaskRepository
) {

    private val _batchProgress = MutableStateFlow<BatchProgress?>(null)
    val batchProgress: StateFlow<BatchProgress?> = _batchProgress.asStateFlow()

    private val _batchResults = MutableStateFlow<List<BatchTaskResult>>(emptyList())
    val batchResults: StateFlow<List<BatchTaskResult>> = _batchResults.asStateFlow()

    private var currentBatchJob: Job? = null
    private var isCancelled = false

    suspend fun executeBatch(
        tasks: List<TaskTemplate>,
        config: BatchExecutionConfig = BatchExecutionConfig(),
        scope: CoroutineScope
    ): Result<BatchExecution> {
        return try {
            val batchId = UUID.randomUUID().toString()
            isCancelled = false

            val initialResults = tasks.map { template ->
                BatchTaskResult(
                    taskId = template.templateId,
                    taskName = template.taskName,
                    status = ExecutionStatus.PENDING
                )
            }
            _batchResults.value = initialResults

            _batchProgress.value = BatchProgress(
                batchId = batchId,
                totalTasks = tasks.size,
                completedTasks = 0,
                failedTasks = 0,
                currentTaskName = null,
                overallProgress = 0f,
                estimatedTimeRemaining = null,
                status = BatchStatus.RUNNING
            )

            currentBatchJob = scope.launch(Dispatchers.Default) {
                executeTasksSequentially(batchId, tasks, config)
            }

            val execution = BatchExecution(
                batchId = batchId,
                taskIds = tasks.map { it.templateId },
                status = BatchStatus.RUNNING,
                totalCount = tasks.size,
                completedCount = 0,
                failedCount = 0,
                pendingCount = tasks.size,
                startedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
                estimatedCompletion = null,
                results = initialResults
            )

            Result.success(execution)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeTasksSequentially(
        batchId: String,
        tasks: List<TaskTemplate>,
        config: BatchExecutionConfig
    ) {
        val results = _batchResults.value.toMutableList()
        var completedCount = 0
        var failedCount = 0
        val startTime = Clock.System.now().toEpochMilliseconds()

        for ((index, template) in tasks.withIndex()) {
            if (isCancelled) {
                updateBatchStatus(batchId, BatchStatus.CANCELLED, results)
                return
            }

            updateCurrentTask(batchId, template.taskName, results)

            val taskResult = executeSingleTask(template, config)
            results[index] = taskResult

            when (taskResult.status) {
                ExecutionStatus.COMPLETED -> completedCount++
                ExecutionStatus.FAILED -> {
                    failedCount++
                    if (config.stopOnFailure) {
                        updateBatchStatus(batchId, BatchStatus.FAILED, results)
                        return
                    }
                }
                else -> {}
            }

            _batchResults.value = results.toList()

            val progress = (index + 1).toFloat() / tasks.size
            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
            val estimatedTotal = (elapsed / (index + 1)) * tasks.size
            val estimatedRemaining = estimatedTotal - elapsed

            _batchProgress.value = _batchProgress.value?.copy(
                completedTasks = completedCount,
                failedTasks = failedCount,
                overallProgress = progress,
                estimatedTimeRemaining = if (index < tasks.size - 1) estimatedRemaining else 0
            )

            if (index < tasks.size - 1) {
                delay(config.delayBetweenTasks)
            }
        }

        val finalStatus = when {
            failedCount == 0 -> BatchStatus.COMPLETED
            completedCount == 0 -> BatchStatus.FAILED
            else -> BatchStatus.PARTIAL
        }

        updateBatchStatus(batchId, finalStatus, results)
    }

    private suspend fun executeSingleTask(
        template: TaskTemplate,
        config: BatchExecutionConfig
    ): BatchTaskResult {
        var attempts = 0
        val maxAttempts = if (config.retryFailed) config.maxRetries + 1 else 1

        while (attempts < maxAttempts) {
            attempts++

            try {
                val result = taskRepository.executeTask(template.templateId)
                result.fold(
                    onSuccess = { execution ->
                        return BatchTaskResult(
                            taskId = template.templateId,
                            taskName = template.taskName,
                            status = ExecutionStatus.COMPLETED,
                            executionId = execution.executionId,
                            progress = 1f,
                            message = "执行成功",
                            completedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                        )
                    },
                    onFailure = { error ->
                        if (attempts >= maxAttempts) {
                            return BatchTaskResult(
                                taskId = template.templateId,
                                taskName = template.taskName,
                                status = ExecutionStatus.FAILED,
                                progress = 0f,
                                message = error.message ?: "执行失败"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                if (attempts >= maxAttempts) {
                    return BatchTaskResult(
                        taskId = template.templateId,
                        taskName = template.taskName,
                        status = ExecutionStatus.FAILED,
                        progress = 0f,
                        message = e.message ?: "执行异常"
                    )
                }
            }

            delay(1000L * attempts)
        }

        return BatchTaskResult(
            taskId = template.templateId,
            taskName = template.taskName,
            status = ExecutionStatus.FAILED,
            progress = 0f,
            message = "达到最大重试次数"
        )
    }

    private fun updateCurrentTask(batchId: String, taskName: String, results: List<BatchTaskResult>) {
        _batchProgress.value = _batchProgress.value?.copy(
            currentTaskName = taskName
        )
    }

    private fun updateBatchStatus(batchId: String, status: BatchStatus, results: List<BatchTaskResult>) {
        _batchProgress.value = _batchProgress.value?.copy(
            status = status,
            currentTaskName = null
        )
    }

    fun cancelBatch() {
        isCancelled = true
        currentBatchJob?.cancel()
        _batchProgress.value = _batchProgress.value?.copy(
            status = BatchStatus.CANCELLED,
            currentTaskName = null
        )
    }

    fun resetState() {
        _batchProgress.value = null
        _batchResults.value = emptyList()
    }

    suspend fun getBatchHistory(): Result<List<BatchExecution>> {
        return try {
            val response = taskApi.getTaskHistory(1, 100)
            if (response.success) {
                Result.success(emptyList())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBatchSummary(): BatchSummary? {
        val progress = _batchProgress.value ?: return null
        val results = _batchResults.value

        return BatchSummary(
            batchId = progress.batchId,
            totalTasks = progress.totalTasks,
            completedTasks = progress.completedTasks,
            failedTasks = progress.failedTasks,
            pendingTasks = progress.totalTasks - progress.completedTasks - progress.failedTasks,
            successRate = if (progress.totalTasks > 0) {
                progress.completedTasks.toFloat() / progress.totalTasks
            } else 0f,
            totalRewardPoints = results
                .filter { it.status == ExecutionStatus.COMPLETED }
                .sumOf { 10.toInt() },
            status = progress.status
        )
    }
}

data class BatchSummary(
    val batchId: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val failedTasks: Int,
    val pendingTasks: Int,
    val successRate: Float,
    val totalRewardPoints: Int,
    val status: BatchStatus
)
