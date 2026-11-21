package com.exantt.btrouterclient

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissedCallsActivity : AppCompatActivity(), ChatRepository.Listener {

    private lateinit var chatRepo: ChatRepository
    private lateinit var missedCallsRoot: LinearLayout
    private lateinit var btnMissedCallsBack: Button
    private lateinit var btnClearMissedCalls: Button
    private lateinit var rvMissedCalls: RecyclerView
    private lateinit var tvMissedCallsEmpty: TextView
    private lateinit var missedCallsAdapter: MissedCallsAdapter

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_missed_calls)

        chatRepo = ChatRepository.getInstance(this)

        missedCallsRoot = findViewById(R.id.missedCallsRoot)
        btnMissedCallsBack = findViewById(R.id.btnMissedCallsBack)
        btnClearMissedCalls = findViewById(R.id.btnClearMissedCalls)
        rvMissedCalls = findViewById(R.id.rvMissedCalls)
        tvMissedCallsEmpty = findViewById(R.id.tvMissedCallsEmpty)

        applySystemInsets()

        btnMissedCallsBack.setOnClickListener { finish() }
        btnClearMissedCalls.setOnClickListener { confirmClearAll() }

        missedCallsAdapter = MissedCallsAdapter { record ->
            val intent = Intent(this, NewSmsActivity::class.java).apply {
                putExtra(NewSmsActivity.EXTRA_PREFILL_NUMBER, record.number)
                if (record.displayName.isNotBlank()) {
                    putExtra(NewSmsActivity.EXTRA_PREFILL_NAME, record.displayName)
                }
            }
            startActivity(intent)
        }
        rvMissedCalls.layoutManager = LinearLayoutManager(this)
        rvMissedCalls.adapter = missedCallsAdapter

        reloadMissedCalls()
    }

    override fun onStart() {
        super.onStart()
        chatRepo.addListener(this)
    }

    override fun onResume() {
        super.onResume()
        BTRouterClientService.markMissedCallsViewed(this)
        reloadMissedCalls()
    }

    override fun onStop() {
        super.onStop()
        chatRepo.removeListener(this)
    }

    override fun onMissedCallsChanged() {
        BTRouterClientService.markMissedCallsViewed(this)
        reloadMissedCalls()
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(missedCallsRoot) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            missedCallsRoot.setPadding(insets.left, insets.top, insets.right, 0)
            rvMissedCalls.setPadding(
                rvMissedCalls.paddingLeft,
                rvMissedCalls.paddingTop,
                rvMissedCalls.paddingRight,
                dpToPx(16) + insets.bottom
            )
            windowInsets
        }
    }

    private fun reloadMissedCalls() {
        val list = chatRepo.getMissedCalls()
        missedCallsAdapter.submitList(list)
        btnClearMissedCalls.isEnabled = list.isNotEmpty()
        btnClearMissedCalls.alpha = if (list.isNotEmpty()) 1.0f else 0.45f
        if (list.isEmpty()) {
            rvMissedCalls.visibility = View.GONE
            tvMissedCallsEmpty.visibility = View.VISIBLE
        } else {
            rvMissedCalls.visibility = View.VISIBLE
            tvMissedCallsEmpty.visibility = View.GONE
        }
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_clear_missed_title)
            .setMessage(R.string.dialog_clear_missed_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                chatRepo.clearMissedCalls()
            }
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private class MissedCallsAdapter(
        private val onSendSmsClick: (ChatLogic.MissedCallRecord) -> Unit
    ) : RecyclerView.Adapter<MissedCallsAdapter.MissedCallViewHolder>() {

        private var items: List<ChatLogic.MissedCallRecord> = emptyList()

        fun submitList(newItems: List<ChatLogic.MissedCallRecord>) {
            val oldItems = items
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldItems.size
                override fun getNewListSize(): Int = newItems.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition].id == newItems[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition] == newItems[newItemPosition]
                }
            })
            items = newItems
            diff.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissedCallViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_missed_call_row, parent, false)
            return MissedCallViewHolder(view)
        }

        override fun onBindViewHolder(holder: MissedCallViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class MissedCallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvCallerName: TextView = itemView.findViewById(R.id.tvMissedCallerName)
            private val tvCallerSub: TextView = itemView.findViewById(R.id.tvMissedCallerSub)
            private val btnSms: ImageButton = itemView.findViewById(R.id.btnMissedCallSms)

            fun bind(record: ChatLogic.MissedCallRecord) {
                val ctx = itemView.context
                val isEng = AppLocale.isEnglish(ctx)
                val locale = Locale.forLanguageTag(AppLocale.getLanguage(ctx))

                val localizedNumber = ServerMessageLocalizer.localizeCallerNumber(record.number, isEng)
                val timeStr = SimpleDateFormat("dd.MM HH:mm", locale).format(Date(record.timestampMs))

                if (record.displayName.isNotBlank()) {
                    tvCallerName.text = record.displayName
                    tvCallerSub.text = "$localizedNumber • $timeStr"
                } else {
                    tvCallerName.text = localizedNumber
                    tvCallerSub.text = timeStr
                }

                if (ChatLogic.isRepliableAddress(record.number)) {
                    btnSms.visibility = View.VISIBLE
                    btnSms.setOnClickListener { onSendSmsClick(record) }
                } else {
                    btnSms.visibility = View.GONE
                    btnSms.setOnClickListener(null)
                }
            }
        }
    }
}
