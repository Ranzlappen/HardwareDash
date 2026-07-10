package dev.ranzlappen.gadget.feature.logbook.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

/**
 * Native [DatePickerDialog] launcher for a checkpoint's due date (date-only
 * — matches the legacy model's `dueDate: "YYYY-MM-DD"` field). No
 * Compose-Material3 `DatePicker` in this codebase yet to mirror, and this
 * module's scope guidance is "keep it simple" — the platform dialog is the
 * lowest-footprint correct option rather than hand-rolling a design-system
 * date picker for a single call site.
 *
 * Returns a launcher function; call it from an `onClick`.
 */
@Composable
fun rememberDueDatePickerLauncher(initialMillis: Long?, onPicked: (Long) -> Unit): () -> Unit {
    val context = LocalContext.current
    return remember(context, initialMillis) {
        {
            val seed = Calendar.getInstance().apply {
                initialMillis?.let { timeInMillis = it }
            }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(picked.timeInMillis)
                },
                seed.get(Calendar.YEAR),
                seed.get(Calendar.MONTH),
                seed.get(Calendar.DAY_OF_MONTH),
            ).show()
        }
    }
}

/**
 * Native date-then-time picker for a checkpoint reminder — a full instant
 * ([onPicked]'s `Long`), since a reminder needs a time-of-day, not just a
 * day. Chains a [DatePickerDialog] into a [TimePickerDialog] rather than a
 * single combined widget (neither platform dialog offers both in one
 * step); the same "no custom date/time picker component exists yet, and
 * this module shouldn't be the one inventing it" reasoning as
 * [rememberDueDatePickerLauncher].
 */
@Composable
fun rememberReminderPickerLauncher(initialMillis: Long?, onPicked: (Long) -> Unit): () -> Unit {
    val context = LocalContext.current
    return remember(context, initialMillis) {
        {
            val seed = Calendar.getInstance().apply {
                initialMillis?.let { timeInMillis = it }
            }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val picked = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth, hourOfDay, minute, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onPicked(picked.timeInMillis)
                        },
                        seed.get(Calendar.HOUR_OF_DAY),
                        seed.get(Calendar.MINUTE),
                        false,
                    ).show()
                },
                seed.get(Calendar.YEAR),
                seed.get(Calendar.MONTH),
                seed.get(Calendar.DAY_OF_MONTH),
            ).show()
        }
    }
}
