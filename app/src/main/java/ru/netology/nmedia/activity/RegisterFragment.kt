package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.databinding.FragmentRegisterBinding
import ru.netology.nmedia.viewmodel.AuthViewModel

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.registerButton.setOnClickListener {
            val name = binding.nameField.text.toString()
            val login = binding.loginField.text.toString()
            val pass = binding.passwordField.text.toString()
            if (name.isBlank() || login.isBlank() || pass.isBlank()) {
                Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.register(login, pass, name)
        }

        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            if (state.token != null) {
                findNavController().navigateUp()
            }
        }

        authViewModel.authError.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }

        return binding.root
    }
}
