package com.universidad.constancias.services;

import com.universidad.constancias.models.Estudiante;
import com.universidad.constancias.repositories.EstudianteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class AutenticacionService implements UserDetailsService {
    private final EstudianteRepository estudiantes;
    private final UserDetails control;

    public AutenticacionService(EstudianteRepository estudiantes, PasswordEncoder encoder,
            @Value("${CONTROL_USUARIO:control}") String usuario,
            @Value("${CONTROL_PASSWORD:}") String contrasena) {
        this.estudiantes = estudiantes;
        this.control = contrasena.isBlank() ? null : User.withUsername(usuario)
                .password(encoder.encode(contrasena)).roles("CONTROL_ESCOLAR").build();
    }

    // buscar las credenciales del usuario
    @Override
    public UserDetails loadUserByUsername(String usuario) {
        if (control != null && control.getUsername().equals(usuario)) {
            return User.withUserDetails(control).build();
        }
        Estudiante estudiante = estudiantes.findById(usuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        String clave = estudiante.getContrasena();
        return User.withUsername(usuario).password(clave == null ? "" : clave)
                .disabled(clave == null).roles("ESTUDIANTE").build();
    }

    public String usuarioActual() {
        Authentication usuario = SecurityContextHolder.getContext().getAuthentication();
        if (usuario == null || !usuario.isAuthenticated() || usuario.getName().equals("anonymousUser")) {
            throw new ResponseStatusException(UNAUTHORIZED, "Autenticación requerida");
        }
        return usuario.getName();
    }

    public boolean esControlEscolar() {
        Authentication usuario = SecurityContextHolder.getContext().getAuthentication();
        if (usuario != null) {
            for (GrantedAuthority permiso : usuario.getAuthorities()) {
                if (permiso.getAuthority().equals("ROLE_CONTROL_ESCOLAR")) {
                    return true;
                }
            }
        }
        return false;
    }

    // cada estudiante opera sobre su matrícula
    public void verificarPermisos(String matricula) {
        if (!esControlEscolar() && !usuarioActual().equals(matricula)) {
            throw new ResponseStatusException(FORBIDDEN, "No tienes permiso para operar sobre otro estudiante");
        }
    }
}
