package com.medha.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.medha.app.data.MedhaDatabase
import com.medha.app.databinding.ActivityConversationLogBinding
import kotlinx.coroutines.launch

class ConversationLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationLogBinding
    private val adapter = ConversationAdapter(emptyList())
    private val dao by lazy { MedhaDatabase.get(this).messageDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                load(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        load("")
    }

    private fun load(query: String) {
        lifecycleScope.launch {
            val items = if (query.isBlank()) dao.recent(200) else dao.search(query)
            adapter.submit(items)
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
