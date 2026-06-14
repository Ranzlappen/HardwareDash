package dev.ranzlappen.gadget.feature.radios.ir.automation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.radios.ir.IrHardware
import dev.ranzlappen.gadget.feature.radios.ir.IrProtocol
import dev.ranzlappen.gadget.feature.radios.ir.IrSignal
import dev.ranzlappen.gadget.feature.radios.ir.IrSignalRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class IrActionHandler @Inject constructor(
    private val hardware: IrHardware,
    private val repository: IrSignalRepository,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_TRANSMIT_IR,
            label = "Transmit IR signal",
            params = listOf(
                ActionParam(PARAM_NAME, ActionParamType.Text, ""),
                ActionParam(PARAM_PROTOCOL, ActionParamType.Text, "NEC"),
                ActionParam(PARAM_PAYLOAD, ActionParamType.Text, ""),
                ActionParam(PARAM_CARRIER_HZ, ActionParamType.Text, "38000"),
                ActionParam(PARAM_REPEATS, ActionParamType.Text, "1"),
            ),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_TRANSMIT_IR -> {
                val name = params[PARAM_NAME]?.takeIf { it.isNotBlank() }
                val signal = if (name != null) {
                    repository.signals.first().firstOrNull {
                        it.name.equals(name, ignoreCase = true)
                    }
                } else null

                val toTransmit = signal ?: run {
                    val payload = params[PARAM_PAYLOAD]?.takeIf { it.isNotBlank() }
                        ?: return ActionResult.Failure("payload is required when no saved signal name is given")
                    val protocol = params[PARAM_PROTOCOL]
                        ?.let { runCatching { IrProtocol.valueOf(it.uppercase()) }.getOrNull() }
                        ?: IrProtocol.NEC
                    val carrierHz = params[PARAM_CARRIER_HZ]?.toIntOrNull() ?: 38_000
                    val repeats = params[PARAM_REPEATS]?.toIntOrNull()?.coerceIn(1, 10) ?: 1
                    IrSignal(
                        id = UUID.randomUUID().toString(),
                        name = "Automation",
                        protocol = protocol,
                        payload = payload,
                        carrierHz = carrierHz,
                        repeats = repeats,
                    )
                }

                val error = hardware.transmit(toTransmit)
                if (error == null) ActionResult.Success else ActionResult.Failure(error)
            }
            else -> ActionResult.Unsupported
        }

    companion object {
        const val FEATURE_ID = "ir"
        const val ACTION_TRANSMIT_IR = "transmit_ir"
        const val PARAM_NAME = "name"
        const val PARAM_PROTOCOL = "protocol"
        const val PARAM_PAYLOAD = "payload"
        const val PARAM_CARRIER_HZ = "carrier_hz"
        const val PARAM_REPEATS = "repeats"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface IrActionModule {

    @Binds
    @IntoMap
    @StringKey(IrActionHandler.FEATURE_ID)
    fun bindIrActionHandler(handler: IrActionHandler): ActionHandler
}
