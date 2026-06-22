package dev.ranzlappen.gadget.feature.bugreport

data class PermissionEntry(
    val label: Int,
    val permission: String,
    val granted: Boolean,
)

data class BugReportState(
    val permissions: List<PermissionEntry> = emptyList(),
    val isRootedFlavor: Boolean = false,
) {
    val missingCount: Int get() = permissions.count { !it.granted }
    val allGranted: Boolean get() = missingCount == 0
}
