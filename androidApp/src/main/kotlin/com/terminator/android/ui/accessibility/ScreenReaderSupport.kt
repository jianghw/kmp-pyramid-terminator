package com.terminator.android.ui.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo

fun Modifier.accessibleHeading(): Modifier = this.semantics { heading() }

fun Modifier.accessibleDescription(description: String): Modifier = this.semantics {
    contentDescription = description
}

fun Modifier.accessibleLiveRegion(): Modifier = this.semantics { liveRegion = LiveRegionMode.Assertive }

fun Modifier.accessibleButton(label: String): Modifier = this.semantics {
    contentDescription = label
    role = Role.Button
}

fun Modifier.accessibleListItem(
    listIndex: Int,
    listSize: Int,
    description: String
): Modifier = this.semantics {
    contentDescription = description
    collectionItemInfo = CollectionItemInfo(
        rowIndex = listIndex,
        rowSpan = 1,
        columnIndex = 0,
        columnSpan = 1
    )
}

fun Modifier.accessibleList(columnCount: Int = 1): Modifier = this.semantics {
    collectionInfo = CollectionInfo(
        rowCount = -1,
        columnCount = columnCount
    )
}

fun Modifier.accessibleSwitch(
    label: String,
    isOn: Boolean
): Modifier = this.semantics {
    contentDescription = label
    role = Role.Switch
    stateDescription = if (isOn) "已开启" else "已关闭"
}

fun Modifier.accessiblePaneTitle(title: String): Modifier = this.semantics {
    paneTitle = title
}

fun Modifier.accessibleTestTag(tag: String): Modifier = this.semantics {
    testTag = tag
}

@Composable
fun AccessibleCard(
    description: String,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = description
                        }
                } else {
                    Modifier.semantics {
                        contentDescription = description
                    }
                }
            ),
        content = content
    )
}
