package com.hairgrowth.os.ui

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.hairgrowth.os.R

/** Dialog used to edit, add or delete a routine (title / description / time). */
object EditDialogs {

    private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun show(
        ctx: Context,
        titleRes: Int,
        initialTitle: String,
        initialDesc: String,
        initialTime: String,
        onSave: (String, String, String) -> Unit,
        onDelete: (() -> Unit)? = null
    ) {
        val pad = Ui.dp(ctx, 20f)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, Ui.dp(ctx, 10f), pad, 0)
        }

        fun field(hint: String, value: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText =
            EditText(ctx).apply {
                this.hint = hint
                setText(value)
                inputType = type
                setTextColor(Palette.TEXT)
                setHintTextColor(Palette.MUTED)
                typeface = Fonts.bn(ctx)
                textSize = 15f
                setSingleLine()
            }

        val titleField = field(ctx.getString(R.string.edit_task_title_label), initialTitle)
        val descField = field(ctx.getString(R.string.edit_task_desc_label), initialDesc)
        val timeField = field(
            ctx.getString(R.string.edit_task_time_label), initialTime,
            InputType.TYPE_CLASS_DATETIME
        )
        col.addView(titleField)
        col.addView(descField)
        col.addView(timeField)

        val builder = AlertDialog.Builder(ctx)
            .setTitle(ctx.getString(titleRes))
            .setView(col)
            .setPositiveButton(ctx.getString(R.string.edit_save), null)
            .setNegativeButton(ctx.getString(R.string.dialog_cancel), null)
        if (onDelete != null) builder.setNeutralButton(ctx.getString(R.string.edit_delete), null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleField.text?.toString()?.trim().orEmpty()
                val desc = descField.text?.toString()?.trim().orEmpty()
                val time = timeField.text?.toString()?.trim().orEmpty()
                if (title.isEmpty() || !TIME.matches(time)) {
                    Toast.makeText(ctx, ctx.getString(R.string.edit_invalid_time), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                onSave(title, desc, time)
                dialog.dismiss()
            }
            if (onDelete != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    onDelete.invoke()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
