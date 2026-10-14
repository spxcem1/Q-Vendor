package com.qvendor.momo.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.qvendor.momo.QVendorApp
import com.qvendor.momo.R
import com.qvendor.momo.data.*
import com.qvendor.momo.databinding.ActivityProfileEditBinding
import kotlinx.coroutines.launch

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private val app by lazy { application as QVendorApp }
    private var editProfileId: Long = -1
    private var selectedIsp: ISP = ISP.MTN
    private var selectedIconIndex: Int = 0
    private var profileActions: MutableList<ProfileAction> = mutableListOf()

    private val iconColors = intArrayOf(
        R.color.qvendor_green,
        R.color.qvendor_mtn,
        R.color.qvendor_telecel,
        R.color.qvendor_airteltigo,
        R.color.qvendor_custom
    )

    private val actionColors = arrayOf(
        "#FF00C853",
        "#FF2E7D32",
        "#FF1B5E20",
        "#FF388E3C",
        "#FF1565C0",
        "#FF6A1B9A",
        "#FFE60000",
        "#FFFFC107",
        "#FFFF6D00",
        "#FF455A64"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        editProfileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)

        setupIspChips()

        if (editProfileId > 0) {
            loadProfile(editProfileId)
        } else {
            profileActions = ISP.MTN.defaultActions.actions.toMutableList()
            renderActionList()
        }

        binding.ivProfileIcon.setOnClickListener { cycleIcon() }
        binding.btnAddAction.setOnClickListener { showActionEditorDialog(null) }
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun setupIspChips() {
        ISP.values().forEach { isp ->
            val chip = Chip(this).apply {
                text = isp.displayName
                isCheckable = true
                setChipBackgroundColorResource(getIspColorResource(isp))
                setTextColor(ContextCompat.getColor(this@ProfileEditActivity, R.color.white))
                id = View.generateViewId()
                setOnClickListener {
                    selectedIsp = isp
                    profileActions = isp.defaultActions.actions.toMutableList()
                    renderActionList()
                }
            }
            binding.chipGroupIsp.addView(chip)
        }
        binding.chipGroupIsp.check(binding.chipGroupIsp.getChildAt(0).id)
    }

    private fun loadProfile(id: Long) {
        lifecycleScope.launch {
            val profile = app.database.profileDao().getProfile(id) ?: return@launch
            binding.etProfileName.setText(profile.name)
            selectedIsp = profile.isp
            selectedIconIndex = profile.iconIndex
            profileActions = profile.profileActions.actions.toMutableList()


            val ispIndex = ISP.values().indexOf(profile.isp)
            if (ispIndex >= 0) {
                binding.chipGroupIsp.check(binding.chipGroupIsp.getChildAt(ispIndex).id)
            }

            updateIcon()
            binding.btnDelete.visibility = View.VISIBLE
            renderActionList()
        }
    }

    private fun cycleIcon() {
        selectedIconIndex = (selectedIconIndex + 1) % iconColors.size
        updateIcon()
    }

    private fun updateIcon() {
        binding.ivProfileIcon.borderColor = ContextCompat.getColor(this, iconColors[selectedIconIndex])
    }

    private fun renderActionList() {
        binding.actionListContainer.removeAllViews()

        profileActions.forEachIndexed { index, action ->
            val itemView = layoutInflater.inflate(R.layout.item_profile_action, binding.actionListContainer, false)

            val colorView = itemView.findViewById<View>(R.id.viewActionColor)
            try {
                colorView.setBackgroundColor(Color.parseColor(action.color))
            } catch (_: Exception) {
                colorView.setBackgroundColor(ContextCompat.getColor(this, R.color.qvendor_green))
            }

            itemView.findViewById<TextView>(R.id.tvActionName).text = action.name
            itemView.findViewById<TextView>(R.id.tvActionCode).text = action.ussdCode

            val stepCount = action.steps.steps.size
            val summary = if (stepCount == 0) "No steps defined" else "$stepCount step(s)"
            itemView.findViewById<TextView>(R.id.tvActionStepsSummary).text = summary

            itemView.findViewById<ImageButton>(R.id.btnEditAction).setOnClickListener {
                showActionEditorDialog(index)
            }

            itemView.findViewById<ImageButton>(R.id.btnRemoveAction).setOnClickListener {
                profileActions.removeAt(index)
                renderActionList()
            }

            binding.actionListContainer.addView(itemView)
        }
    }

    private fun showActionEditorDialog(editIndex: Int?) {
        val view = layoutInflater.inflate(R.layout.dialog_action_editor, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editIndex != null) getString(R.string.edit_action) else getString(R.string.add_action))
            .setView(view)
            .setCancelable(true)
            .create()

        val etName = view.findViewById<TextInputEditText>(R.id.etActionName)
        val etCode = view.findViewById<TextInputEditText>(R.id.etActionCode)
        val stepContainer = view.findViewById<LinearLayout>(R.id.stepListContainer)

        val existingAction = editIndex?.let { profileActions[it] }
        val steps = existingAction?.steps?.steps?.toMutableList() ?: mutableListOf()

        existingAction?.let {
            etName.setText(it.name)
            etCode.setText(it.ussdCode)
        }

        fun renderSteps() {
            stepContainer.removeAllViews()
            steps.forEachIndexed { index, step ->
                val itemView = layoutInflater.inflate(R.layout.item_ussd_step, stepContainer, false)

                itemView.findViewById<TextView>(R.id.tvStepNumber).text =
                    getString(R.string.step_number, index + 1)

                val etInput = itemView.findViewById<TextInputEditText>(R.id.etStepInput)

                etInput.setText(step.input)

                etInput.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        steps[index] = steps[index].copy(input = s?.toString() ?: "")
                    }
                })

                itemView.findViewById<ImageButton>(R.id.btnRemoveStep).setOnClickListener {
                    steps.removeAt(index)
                    renderSteps()
                }

                stepContainer.addView(itemView)
            }
        }

        renderSteps()

        view.findViewById<View>(R.id.btnAddStep).setOnClickListener {
            steps.add(UssdStep("", 1500, ""))
            renderSteps()
        }

        view.findViewById<View>(R.id.btnEditorSave).setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val code = etCode.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                Toast.makeText(this, "Action name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (code.isBlank()) {
                Toast.makeText(this, "USSD code is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val actionColor = if (editIndex != null && editIndex < profileActions.size) {
                profileActions[editIndex].color
            } else {
                actionColors[profileActions.size % actionColors.size]
            }

            val collectedSteps = steps.toList()

            val action = ProfileAction(
                name = name,
                ussdCode = code,
                steps = UssdStepScript(collectedSteps),
                color = actionColor,
                requiresParams = false,
                pinIndex = -1,
                amountIndex = -1,
                phoneIndex = -1,
                referenceIndex = -1
            )

            if (editIndex != null) {
                profileActions[editIndex] = action
            } else {
                profileActions.add(action)
            }
            renderActionList()
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnEditorCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveProfile() {
        val name = binding.etProfileName.text.toString().trim()
        if (name.isBlank()) {
            binding.tilProfileName.error = "Required"
            return
        }

        val actionsJson = ProfileActions.toJson(ProfileActions(profileActions.toList()))

        lifecycleScope.launch {
            val dao = app.database.profileDao()
            if (editProfileId > 0) {
                val existing = dao.getProfile(editProfileId) ?: return@launch
                val updated = existing.copy(
                    name = name,
                    ispName = selectedIsp.name,
                    iconIndex = selectedIconIndex,
                    actionsJson = actionsJson
                )
                dao.update(updated)
            } else {
                val all = dao.getAllProfilesSync()
                val isFirst = all.isEmpty()
                val profile = VendorProfile(
                    name = name,
                    ispName = selectedIsp.name,
                    iconIndex = selectedIconIndex,
                    isActive = isFirst,
                    actionsJson = actionsJson
                )
                dao.insert(profile)
            }
            Toast.makeText(this@ProfileEditActivity, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmDelete() {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<View>(R.id.btnDialogYes).setOnClickListener {
            lifecycleScope.launch {
                val profile = app.database.profileDao().getProfile(editProfileId) ?: return@launch
                app.database.profileDao().delete(profile)
                Toast.makeText(this@ProfileEditActivity, "Deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnDialogNo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getIspColorResource(isp: ISP): Int = when (isp) {
        ISP.MTN -> R.color.qvendor_mtn
        ISP.TELECEL -> R.color.qvendor_telecel
        ISP.AIRTELTIGO -> R.color.qvendor_airteltigo
        ISP.CUSTOM -> R.color.qvendor_custom
    }

    companion object {
        const val EXTRA_PROFILE_ID = "extra_profile_id"
    }
}
