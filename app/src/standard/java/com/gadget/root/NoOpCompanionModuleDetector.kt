package com.gadget.root

import com.gadget.root.companion.CompanionModuleConstants
import com.gadget.root.companion.CompanionModuleDetector
import com.gadget.root.companion.CompanionModuleStatus

class NoOpCompanionModuleDetector : CompanionModuleDetector {
    override suspend fun status(): CompanionModuleStatus = CompanionModuleStatus.NotInstalled
    override val installInstructionsUrl: String = CompanionModuleConstants.InstallInstructionsUrl
    override val moduleId: String = CompanionModuleConstants.ModuleId
}
