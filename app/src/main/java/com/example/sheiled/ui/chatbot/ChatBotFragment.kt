package com.example.sheiled.ui.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sheiled.databinding.FragmentChatbotBinding

class ChatBotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChatbotBinding.inflate(inflater, container, false)

        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = adapter

        // Welcome message
        addBotMessage(
            "Hi 👋 I’m SHEiled Assistant.\n\n" +
                    "Ask me how to use the app, safety tips, or anything."
        )

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                binding.etMessage.setText("")
                askAI()
            }
        }

        return binding.root
    }

    private fun askAI() {
        GroqService.askGroq(
            messages,
            callback = { reply ->
                requireActivity().runOnUiThread {
                    addBotMessage(reply)
                }
            },
            onError = {
                requireActivity().runOnUiThread {
                    addBotMessage("Sorry, I couldn’t process that right now.")
                }
            }
        )
    }

    private fun addUserMessage(msg: String) {
        messages.add(ChatMessage(msg, true))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(msg: String) {
        messages.add(ChatMessage(msg, false))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
