package com.gadget.root

import dev.ranzlappen.gadget.core.root.companion.CompanionModuleConstants
import dev.ranzlappen.gadget.core.root.companion.CompanionModuleDetector
import dev.ranzlappen.gadget.core.root.companion.CompanionModuleStatus

class NoOpCompanionModuleDetector : CompanionModuleDetector {
    override suspend fun status(): CompanionModuleStatus = CompanionModuleStatus.NotInstalled
    override val installInstructionsUrl: String = CompanionModuleConstants.InstallInstructionsUrl
    override val moduleId: String = CompanionModuleConstants.ModuleId
}
