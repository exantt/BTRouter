package com.exantt.btrouterclient

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArchivedChatsActivity : AppCompatActivity(), ChatRepository.Listener {

    private lateinit var chatRepo: ChatRepository
    private lateinit var archivedRoot: LinearLayout
    private lateinit var btnArchivedBack: Button
    private lateinit var rvArchivedChats: RecyclerView
    private lateinit var tvArchivedEmpty: TextView
    private lateinit var chatAdapter: ChatListAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_archived_chats)

        chatRepo = ChatRepository.getInstance(this)

        archivedRoot = findViewById(R.id.archivedRoot)
        btnArchivedBack = findViewById(R.id.btnArchivedBack)
        rvArchivedChats = findViewById(R.id.rvArchivedChats)
        tvArchivedEmpty = findViewById(R.id.tvArchivedEmpty)

        applySystemInsets()

        btnArchivedBack.setOnClickListener { finish() }

        chatAdapter = ChatListAdapter(
            onClick = { chat ->
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CHAT_KEY, chat.chatKey)
                })
            },
            onLongClick = { chat ->
                showChatActionsDialog(chat)
            }
        )
        rvArchivedChats.layoutManager = LinearLayoutManager(this)
        rvArchivedChats.adapter = chatAdapter

        reloadChats()
    }

    override fun onStart() {
        super.onStart()
        chatRepo.addListener(this)
    }

    override fun onResume() {
        super.onResume()
        reloadChats()
    }

    override fun onStop() {
        super.onStop()
        chatRepo.removeListener(this)
    }

    override fun onChatsChanged(chatKey: String?) {
        reloadChats()
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(archivedRoot) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            archivedRoot.setPadding(insets.left, insets.top, insets.right, 0)
            rvArchivedChats.setPadding(
                rvArchivedChats.paddingLeft,
                rvArchivedChats.paddingTop,
                rvArchivedChats.paddingRight,
                dpToPx(16) + insets.bottom
            )
            windowInsets
        }
    }

    private fun reloadChats() {
        val archived = chatRepo.getArchivedChats()
        chatAdapter.submitList(archived)
        if (archived.isEmpty()) {
            rvArchivedChats.visibility = View.GONE
            tvArchivedEmpty.visibility = View.VISIBLE
        } else {
            rvArchivedChats.visibility = View.VISIBLE
            tvArchivedEmpty.visibility = View.GONE
        }
    }

    private fun showChatActionsDialog(chat: ChatLogic.ChatThread) {
        val isEng = AppLocale.isEnglish(this)
        val title = if (chat.displayName.isNotBlank()) {
            chat.displayName
        } else {
            ServerMessageLocalizer.localizeSenderNumber(chat.address, isEng)
        }
        val items = arrayOf(
            getString(R.string.chat_action_unarchive),
            getString(R.string.chat_action_delete)
        )
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        chatRepo.setArchived(chat.chatKey, false)
                        Toast.makeText(this, getString(R.string.toast_chat_unarchived), Toast.LENGTH_SHORT).show()
                    }
                    1 -> confirmDeleteChat(chat, title)
                }
            }
            .show()
    }

    private fun confirmDeleteChat(chat: ChatLogic.ChatThread, title: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_chat_title))
            .setMessage("$title\n\n${getString(R.string.dialog_delete_chat_message)}")
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                chatRepo.deleteChat(chat.chatKey)
                Toast.makeText(this, getString(R.string.toast_chat_deleted), Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
