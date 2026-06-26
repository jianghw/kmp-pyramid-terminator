package com.terminator.shared.network.api

import com.terminator.shared.model.ApiResponse
import com.terminator.shared.model.PaginatedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * AI网络API客户端 - 封装所有AI相关的HTTP请求
 *
 * 这个类是前端（Android/iOS/Web）与后端AI服务通信的桥梁。
 * 它把复杂的HTTP请求封装成简单的方法调用，使用者不需要关心底层的网络细节。
 *
 * 工作流程：
 * 1. 前端UI调用 AIApi 的方法（如 askQuestion()）
 * 2. AIApi 把参数打包成JSON，发送HTTP请求到后端
 * 3. 后端处理请求，返回JSON响应
 * 4. AIApi 把JSON响应解析成数据对象，返回给前端UI
 *
 * 使用 Kotlin 的 suspend 关键字标记为挂起函数，
 * 因为网络请求是异步的，不会阻塞主线程（避免界面卡顿）。
 *
 * @param httpClient HTTP客户端实例，负责实际的网络通信
 */
class AIApi(private val httpClient: HttpClient) {

    /**
     * 向AI提问
     *
     * 发送一道题目给后端，后端会调用AI大模型接口获取答案。
     *
     * @param question 题目内容（必填）
     * @param context 上下文信息（可选，提供背景知识帮助AI理解题目）
     * @param options 选项列表（可选，选择题时提供）
     * @param questionType 题目类型（如 "single_choice", "judgment"）
     * @param configId 指定使用的AI配置ID（可选，不指定则使用默认配置）
     * @return 包含AI答案、置信度和解析的响应
     */
    suspend fun askQuestion(
        question: String,
        context: String?,
        options: List<String>?,
        questionType: String,
        configId: Long?
    ): ApiResponse<AIQuestionResponse> {
        // 发送POST请求到 /api/ai/ask
        // setBody() 会自动将数据对象序列化为JSON
        return httpClient.post("/api/ai/ask") {
            setBody(AIQuestionRequest(
                question = question,
                context = context,
                options = options,
                questionType = questionType,
                configId = configId
            ))
        }.body()  // .body() 将HTTP响应体反序列化为指定类型
    }

    /**
     * 从题库中搜索题目
     *
     * 在本地题库中查找匹配的题目，返回标准答案。
     * 这个方法不调用AI接口，查询速度快。
     *
     * @param keyword 搜索关键词
     * @param bankId 指定题库ID（可选）
     * @param questionType 指定题目类型过滤（可选）
     * @return 匹配的题目信息和答案
     */
    suspend fun searchQuestion(
        keyword: String,
        bankId: Long?,
        questionType: String?
    ): ApiResponse<QuestionEntryResponse> {
        return httpClient.post("/api/ai/search") {
            setBody(SearchQuestionRequest(
                keyword = keyword,
                bankId = bankId,
                questionType = questionType
            ))
        }.body()
    }

    // ==========================================
    // AI配置管理相关接口
    // ==========================================

    /**
     * 获取所有AI配置列表
     *
     * @return 当前用户的所有AI配置（API密钥已脱敏处理）
     */
    suspend fun getAIConfigs(): ApiResponse<List<AIConfigResponse>> {
        return httpClient.get("/api/ai-configs").body()
    }

    /**
     * 获取单个AI配置详情
     *
     * @param configId 配置ID
     * @return 指定配置的详细信息
     */
    suspend fun getAIConfig(configId: Long): ApiResponse<AIConfigResponse> {
        return httpClient.get("/api/ai-configs/$configId").body()
    }

    /**
     * 创建新的AI配置
     *
     * @param request 包含提供商、API密钥、模型等信息的请求对象
     * @return 创建成功后的配置信息
     */
    suspend fun createAIConfig(request: CreateAIConfigRequest): ApiResponse<AIConfigResponse> {
        return httpClient.post("/api/ai-configs") {
            setBody(request)
        }.body()
    }

    /**
     * 更新AI配置
     *
     * @param configId 要更新的配置ID
     * @param request 更新后的配置信息
     * @return 操作结果
     */
    suspend fun updateAIConfig(configId: Long, request: CreateAIConfigRequest): ApiResponse<Unit> {
        return httpClient.put("/api/ai-configs/$configId") {
            setBody(request)
        }.body()
    }

    /**
     * 删除AI配置
     *
     * @param configId 要删除的配置ID
     * @return 操作结果
     */
    suspend fun deleteAIConfig(configId: Long): ApiResponse<Unit> {
        return httpClient.delete("/api/ai-configs/$configId").body()
    }

    /**
     * 切换AI配置的启用/禁用状态
     *
     * @param configId 要切换状态的配置ID
     * @return 操作结果
     */
    suspend fun toggleAIConfig(configId: Long): ApiResponse<Unit> {
        return httpClient.put("/api/ai-configs/$configId/toggle").body()
    }

    // ==========================================
    // 题库管理相关接口
    // ==========================================

    /**
     * 获取所有题库列表
     *
     * @return 当前用户的所有题库
     */
    suspend fun getQuestionBanks(): ApiResponse<List<QuestionBankResponse>> {
        return httpClient.get("/api/question-banks").body()
    }

    /**
     * 获取单个题库详情
     *
     * @param bankId 题库ID
     * @return 指定题库的详细信息
     */
    suspend fun getQuestionBank(bankId: Long): ApiResponse<QuestionBankResponse> {
        return httpClient.get("/api/question-banks/$bankId").body()
    }

    /**
     * 创建新题库
     *
     * @param request 包含题库名称和描述的请求对象
     * @return 创建成功后的题库信息
     */
    suspend fun createQuestionBank(request: CreateQuestionBankRequest): ApiResponse<QuestionBankResponse> {
        return httpClient.post("/api/question-banks") {
            setBody(request)
        }.body()
    }

    /**
     * 更新题库信息
     *
     * @param bankId 要更新的题库ID
     * @param request 更新后的题库信息
     * @return 操作结果
     */
    suspend fun updateQuestionBank(bankId: Long, request: CreateQuestionBankRequest): ApiResponse<Unit> {
        return httpClient.put("/api/question-banks/$bankId") {
            setBody(request)
        }.body()
    }

    /**
     * 删除题库
     *
     * @param bankId 要删除的题库ID
     * @return 操作结果
     */
    suspend fun deleteQuestionBank(bankId: Long): ApiResponse<Unit> {
        return httpClient.delete("/api/question-banks/$bankId").body()
    }

    /**
     * 获取题库中的所有题目
     *
     * @param bankId 题库ID
     * @return 题库中的所有题目列表
     */
    suspend fun getQuestionEntries(bankId: Long): ApiResponse<List<QuestionEntryResponse>> {
        return httpClient.get("/api/question-banks/$bankId/entries").body()
    }

    /**
     * 向题库添加新题目
     *
     * @param bankId 目标题库ID
     * @param request 包含题目信息的请求对象
     * @return 添加成功后的题目信息
     */
    suspend fun createQuestionEntry(bankId: Long, request: CreateQuestionEntryRequest): ApiResponse<QuestionEntryResponse> {
        return httpClient.post("/api/question-banks/$bankId/entries") {
            setBody(request)
        }.body()
    }

    /**
     * 从题库中删除题目
     *
     * @param bankId 题库ID
     * @param entryId 要删除的题目ID
     * @return 操作结果
     */
    suspend fun deleteQuestionEntry(bankId: Long, entryId: Long): ApiResponse<Unit> {
        return httpClient.delete("/api/question-banks/$bankId/entries/$entryId").body()
    }
}

// ==========================================
// 请求/响应数据类定义
// ==========================================

/**
 * AI提问请求 - 发送给后端的题目数据
 *
 * @param question 题目内容
 * @param context 上下文（可选）
 * @param options 选项列表（可选，选择题用）
 * @param questionType 题目类型
 * @param configId 指定AI配置ID（可选）
 */
@kotlinx.serialization.Serializable
data class AIQuestionRequest(
    val question: String,
    val context: String? = null,
    val options: List<String>? = null,
    @kotlinx.serialization.SerialName("question_type")
    val questionType: String,
    @kotlinx.serialization.SerialName("config_id")
    val configId: Long? = null
)

/**
 * AI提问响应 - 后端返回的AI答案
 *
 * @param answer AI给出的答案
 * @param confidence 置信度（0.0-1.0）
 * @param explanation 答案解析（可选）
 * @param provider 使用的AI提供商名称
 */
@kotlinx.serialization.Serializable
data class AIQuestionResponse(
    val answer: String,
    val confidence: Double,
    val explanation: String? = null,
    val provider: String
)

/**
 * 搜索题目请求 - 在题库中搜索题目
 *
 * @param keyword 搜索关键词
 * @param bankId 指定题库ID（可选）
 * @param questionType 题目类型过滤（可选）
 */
@kotlinx.serialization.Serializable
data class SearchQuestionRequest(
    val keyword: String,
    @kotlinx.serialization.SerialName("bank_id")
    val bankId: Long? = null,
    @kotlinx.serialization.SerialName("question_type")
    val questionType: String? = null
)

/**
 * AI配置响应 - 后端返回的AI配置信息（密钥已脱敏）
 */
@kotlinx.serialization.Serializable
data class AIConfigResponse(
    @kotlinx.serialization.SerialName("config_id")
    val configId: Long,
    @kotlinx.serialization.SerialName("user_id")
    val userId: Long,
    val provider: String,
    @kotlinx.serialization.SerialName("api_key_masked")
    val apiKeyMasked: String,         // 脱敏后的API密钥（如 sk-12****7890）
    @kotlinx.serialization.SerialName("model_name")
    val modelName: String,
    @kotlinx.serialization.SerialName("base_url")
    val baseUrl: String,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Double,
    @kotlinx.serialization.SerialName("is_enabled")
    val isEnabled: Boolean,
    @kotlinx.serialization.SerialName("created_at")
    val createdAt: String,
    @kotlinx.serialization.SerialName("updated_at")
    val updatedAt: String
)

/**
 * 创建/更新AI配置请求
 *
 * @param provider AI提供商（如 "openai", "qwen"）
 * @param apiKey API密钥
 * @param modelName 模型名称
 * @param baseUrl API接口地址
 * @param maxTokens 最大token数（默认1000）
 * @param temperature 温度参数（默认0.7）
 */
@kotlinx.serialization.Serializable
data class CreateAIConfigRequest(
    val provider: String,
    @kotlinx.serialization.SerialName("api_key")
    val apiKey: String,
    @kotlinx.serialization.SerialName("model_name")
    val modelName: String,
    @kotlinx.serialization.SerialName("base_url")
    val baseUrl: String,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7
)

/**
 * 题库响应 - 后端返回的题库信息
 */
@kotlinx.serialization.Serializable
data class QuestionBankResponse(
    @kotlinx.serialization.SerialName("bank_id")
    val bankId: Long,
    @kotlinx.serialization.SerialName("user_id")
    val userId: Long,
    @kotlinx.serialization.SerialName("bank_name")
    val bankName: String,
    val description: String,
    @kotlinx.serialization.SerialName("question_count")
    val questionCount: Int,
    @kotlinx.serialization.SerialName("created_at")
    val createdAt: String,
    @kotlinx.serialization.SerialName("updated_at")
    val updatedAt: String
)

/**
 * 创建题库请求
 */
@kotlinx.serialization.Serializable
data class CreateQuestionBankRequest(
    @kotlinx.serialization.SerialName("bank_name")
    val bankName: String,
    val description: String
)

/**
 * 题目响应 - 后端返回的题目详细信息
 */
@kotlinx.serialization.Serializable
data class QuestionEntryResponse(
    @kotlinx.serialization.SerialName("entry_id")
    val entryId: Long,
    @kotlinx.serialization.SerialName("bank_id")
    val bankId: Long,
    val question: String,
    @kotlinx.serialization.SerialName("question_type")
    val questionType: String,
    val options: List<String>?,
    @kotlinx.serialization.SerialName("correct_answer")
    val correctAnswer: String,
    val explanation: String?,
    val tags: List<String>,
    @kotlinx.serialization.SerialName("created_at")
    val createdAt: String
)

/**
 * 创建题目请求
 */
@kotlinx.serialization.Serializable
data class CreateQuestionEntryRequest(
    @kotlinx.serialization.SerialName("bank_id")
    val bankId: Long,
    val question: String,
    @kotlinx.serialization.SerialName("question_type")
    val questionType: String,
    val options: List<String>? = null,
    @kotlinx.serialization.SerialName("correct_answer")
    val correctAnswer: String,
    val explanation: String? = null,
    val tags: List<String> = emptyList()
)
