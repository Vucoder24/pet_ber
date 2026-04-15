package com.nvv.petber.ui.mention

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class MentionEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    interface OnMentionRemovedListener {
        fun onMentionRemoved(petId: String)
    }

    var mentionRemovedListener: OnMentionRemovedListener? = null
    private var isDeletingSpan = false
    private var isInternalChange = false

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        super.onTextChanged(text, start, before, count)

        if (isDeletingSpan || isInternalChange) return

        val editable = text as? Editable ?: return

        val spans = editable.getSpans(0, editable.length, MentionSpan::class.java)

        for (span in spans) {
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)

            if (start in spanStart until spanEnd) {
                isDeletingSpan = true

                editable.removeSpan(span)
                editable.delete(spanStart, spanEnd)

                mentionRemovedListener?.onMentionRemoved(span.petId)

                isDeletingSpan = false
                break
            }
        }
    }

    fun insertMention(petId: String, petName: String) {
        val editable = text ?: return

        if (hasMention(petId)) return

        val mentionText = "@$petName "
        val start = selectionStart.coerceAtLeast(0)

        editable.insert(start, mentionText)

        editable.setSpan(
            MentionSpan(petId, petName),
            start,
            start + mentionText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun hasMention(petId: String): Boolean {
        val spannable = text as? Spannable ?: return false

        val spans = spannable.getSpans(0, spannable.length, MentionSpan::class.java)

        return spans.any { it.petId == petId }
    }

    fun getMentions(): List<String> {
        val spannable = text as? Spannable ?: return emptyList()

        val spans = spannable.getSpans(0, spannable.length, MentionSpan::class.java)

        return spans.map { it.petId }
    }

    fun removeMention(petId: String) {
        val editable = text as? Editable ?: return

        val spans = editable.getSpans(0, editable.length, MentionSpan::class.java)

        for (span in spans) {
            if (span.petId == petId) {
                val start = editable.getSpanStart(span)
                val end = editable.getSpanEnd(span)

                isInternalChange = true

                editable.removeSpan(span)
                editable.delete(start, end)

                isInternalChange = false
                break
            }
        }
    }
}