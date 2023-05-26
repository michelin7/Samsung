package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication2.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var requestInput: TextInputEditText
    private lateinit var podsAdapter: PodsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var waEngine: WAEngine
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady: Boolean = false
    private val VOICE_RECOGNITION_REQUEST_CODE: Int = 713
    private lateinit var viewModel: MainActivityViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        initTts()
        initViews()
        initWolframEngine()
    }

    private fun initViews() {
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        requestInput = binding.textInputEdit
        requestInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.pods.clear()
                podsAdapter.notifyDataSetChanged()

                val question = requestInput.text.toString()
                wolframRequest(question)
            }

            return@setOnEditorActionListener false
        }


        podsAdapter = PodsAdapter(viewModel.pods, object : PodsOnClickListener {
            override fun onClicked(Pod: Pod) {
                if (isTtsReady) {
                    textToSpeech.language = Locale.US
                    textToSpeech.speak(Pod.content, TextToSpeech.QUEUE_FLUSH, null, Pod.title)
                }
            }
        })

        val podsRV = binding.podsList
        podsRV.layoutManager = LinearLayoutManager(this)
        podsRV.adapter = podsAdapter


        val voiceInputButton = binding.voiceInputButton
        voiceInputButton.setOnClickListener {
            viewModel.pods.clear()
            podsAdapter.notifyDataSetChanged()

            if (isTtsReady) {
                textToSpeech.stop()
            }

            showVoiceInputDialog()
        }

        progressBar = binding.progressBar

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_stop -> {
                if (isTtsReady) {
                    textToSpeech.stop()
                }
                return true
            }
            R.id.action_clear -> {
                requestInput.text?.clear()
                viewModel.pods.clear()
                podsAdapter.notifyDataSetChanged()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initWolframEngine() {
        waEngine = WAEngine().apply {
            appID = "X3JR6A-2QR8LW9T4V"
            addFormat("plaintext")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .apply {
                setAction(android.R.string.ok) {
                    dismiss()
                }
                show()
            }
    }

    private fun wolframRequest(request: String) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val query = waEngine.createQuery().apply { input = request }
            runCatching {
                waEngine.performQuery(query)
            }.onSuccess { result ->
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (result.isError) {
                        showSnackbar(result.errorMessage)
                        return@withContext
                    }

                    if (!result.isSuccess) {
                        requestInput.error = getString(R.string.error_do_not_understand)
                        return@withContext
                    }

                    for (pod in result.pods) {
                        if (pod.isError) continue
                        val content = StringBuilder()
                        for (subPod in pod.subpods) {
                            for (elem in subPod.contents) {
                                if (elem is WAPlainText) {
                                    content.append(elem.text)
                                }
                            }
                        }
                        viewModel.pods.add(Pod(pod.title, content.toString()))
                    }

                    podsAdapter.notifyDataSetChanged()
                }
            }.onFailure { t ->
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showSnackbar(t.message ?: getString(R.string.error_smth_went_wrong))
                }
            }
        }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(this) { code ->
            if (code != TextToSpeech.SUCCESS) {
                showSnackbar(getString(R.string.error_tts_is_not_ready))
            } else {
                isTtsReady = true
            }
        }
    }

    private fun showVoiceInputDialog() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.request_hint))
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        }

        kotlin.runCatching {
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
        }.onFailure { t ->
            showSnackbar(t.message ?: getString(R.string.error_voice_recognition_unavailable))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.let { question ->
                requestInput.setText(question)
                wolframRequest(question)
            }
        }
    }
}