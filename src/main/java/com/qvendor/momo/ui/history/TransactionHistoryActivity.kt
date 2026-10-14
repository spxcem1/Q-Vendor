package com.qvendor.momo.ui.history

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.qvendor.momo.QVendorApp
import com.qvendor.momo.R
import com.qvendor.momo.databinding.ActivityTransactionHistoryBinding
import kotlinx.coroutines.launch

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private val app by lazy { application as QVendorApp }
    private val adapter = TransactionAdapter { transaction ->
        Toast.makeText(
            this,
            "${transaction.actionName} · ${transaction.status.name}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private var profileId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileId = intent.getLongExtra(EXTRA_PROFILE_ID, 0L)
        resolveTitle()

        binding.recyclerTransactions.layoutManager = LinearLayoutManager(this)
        binding.recyclerTransactions.adapter = adapter

        binding.btnClearHistory.setOnClickListener { confirmClear() }

        val dao = app.database.transactionDao()
        val source = if (profileId > 0) dao.getForProfile(profileId) else dao.getAll()
        source.observe(this) { list ->
            adapter.submit(list)
            binding.tvEmpty.visibility =
                if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun resolveTitle() {
        if (profileId <= 0) {
            binding.historyTitle.setText(R.string.transaction_history_title)
            return
        }
        binding.historyTitle.text = getString(R.string.transaction_history_title)
        lifecycleScope.launch {
            val profile = app.database.profileDao().getProfile(profileId) ?: return@launch
            val name = "${profile.isp.displayName} · ${profile.name}"
            binding.historyTitle.text = name
        }
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_history)
            .setMessage(R.string.confirm_clear_history)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    if (profileId > 0) {
                        app.database.transactionDao().clearForProfile(profileId)
                    } else {
                        app.database.transactionDao().clear()
                    }
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    companion object {
        const val EXTRA_PROFILE_ID = "extra_profile_id"
    }
}
