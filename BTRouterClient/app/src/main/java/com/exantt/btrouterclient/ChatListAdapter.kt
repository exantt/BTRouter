package com.exantt.btrouterclient

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ChatListAdapter(
    private val onClick: (ChatLogic.ChatThread) -> Unit,
    private val onLongClick: (ChatLogic.ChatThread) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    companion object {
        private val AVATAR_PALETTE = intArrayOf(
            Color.parseColor("#9E82D9"),
            Color.parseColor("#E57399"),
            Color.parseColor("#4DB6AC"),
            Color.parseColor("#64B5F6"),
            Color.parseColor("#FFB74D"),
            Color.parseColor("#81C784"),
            Color.parseColor("#BA68C8"),
            Color.parseColor("#4FC3F7")
        )
        private val NEUTRAL_AVATAR_BG = Color.parseColor("#49494D")
    }

    private var items: List<ChatLogic.ChatThread> = emptyList()

    fun submitList(newItems: List<ChatLogic.ChatThread>) {
        val oldItems = items
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].chatKey == newItems[newItemPosition].chatKey
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition] == newItems[newItemPosition]
            }
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_row, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatarBg: ImageView = itemView.findViewById(R.id.ivAvatarBg)
        private val tvAvatarLetter: TextView = itemView.findViewById(R.id.tvAvatarLetter)
        private val ivAvatarIcon: ImageView = itemView.findViewById(R.id.ivAvatarIcon)
        private val ivChatPin: ImageView = itemView.findViewById(R.id.ivChatPin)
        private val tvChatTitle: TextView = itemView.findViewById(R.id.tvChatTitle)
        private val tvChatNewBadge: TextView = itemView.findViewById(R.id.tvChatNewBadge)
        private val tvChatPreview: TextView = itemView.findViewById(R.id.tvChatPreview)
        private val tvChatTime: TextView = itemView.findViewById(R.id.tvChatTime)
        private val tvChatUnreadCount: TextView = itemView.findViewById(R.id.tvChatUnreadCount)

        fun bind(chat: ChatLogic.ChatThread) {
            val ctx = itemView.context
            val isEng = AppLocale.isEnglish(ctx)
            val locale = Locale.forLanguageTag(AppLocale.getLanguage(ctx))

            val title = if (chat.displayName.isNotBlank()) {
                chat.displayName
            } else {
                ServerMessageLocalizer.localizeSenderNumber(chat.address, isEng)
            }
            tvChatTitle.text = title

            val initial = ChatLogic.avatarInitial(chat.displayName, chat.address)
            if (initial != null) {
                val idx = ChatLogic.avatarColorIndex(chat.chatKey, AVATAR_PALETTE.size)
                ivAvatarBg.imageTintList = ColorStateList.valueOf(AVATAR_PALETTE[idx])
                tvAvatarLetter.text = initial
                tvAvatarLetter.visibility = View.VISIBLE
                ivAvatarIcon.visibility = View.GONE
            } else {
                ivAvatarBg.imageTintList = ColorStateList.valueOf(NEUTRAL_AVATAR_BG)
                tvAvatarLetter.visibility = View.GONE
                ivAvatarIcon.visibility = View.VISIBLE
            }

            ivChatPin.visibility = if (chat.isPinned) View.VISIBLE else View.GONE

            val hasUnread = chat.unreadCount > 0
            tvChatNewBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
            if (hasUnread) {
                tvChatUnreadCount.text = chat.unreadCount.toString()
                tvChatUnreadCount.visibility = View.VISIBLE
            } else {
                tvChatUnreadCount.visibility = View.GONE
            }

            val previewBody = chat.lastMessageBody.replace('\n', ' ').trim()
            val previewText = if (chat.lastMessageIncoming) {
                previewBody
            } else {
                ctx.getString(R.string.chat_preview_you_prefix, previewBody)
            }
            tvChatPreview.text = previewText

            tvChatTitle.setTypeface(null, if (hasUnread) Typeface.BOLD else Typeface.NORMAL)
            tvChatTitle.setTextColor(if (hasUnread) Color.WHITE else Color.parseColor("#E3E3E6"))
            tvChatPreview.setTypeface(null, if (hasUnread) Typeface.BOLD else Typeface.NORMAL)
            tvChatPreview.setTextColor(if (hasUnread) Color.parseColor("#E3E3E6") else Color.parseColor("#9AA0A6"))
            tvChatTime.setTypeface(null, if (hasUnread) Typeface.BOLD else Typeface.NORMAL)
            tvChatTime.setTextColor(if (hasUnread) Color.parseColor("#E3E3E6") else Color.parseColor("#9AA0A6"))

            tvChatTime.text = ChatLogic.formatChatListTime(
                chat.lastMessageTs,
                System.currentTimeMillis(),
                locale
            )

            itemView.setOnClickListener { onClick(chat) }
            itemView.setOnLongClickListener {
                onLongClick(chat)
                true
            }
        }
    }
}
