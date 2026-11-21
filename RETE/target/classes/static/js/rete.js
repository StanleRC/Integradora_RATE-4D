// ===== Script compartido RETE / AbModel =====

// Menú hamburguesa: abrir/cerrar, cerrar al hacer clic fuera o con Escape
(function () {
    const btn = document.getElementById('btn-menu');
    const menu = document.getElementById('menu-nav');
    if (btn && menu) {
        const cerrar = () => {
            menu.classList.remove('abierto');
            btn.setAttribute('aria-expanded', 'false');
        };
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const abierto = menu.classList.toggle('abierto');
            btn.setAttribute('aria-expanded', abierto);
        });
        document.addEventListener('click', (e) => { if (!menu.contains(e.target)) cerrar(); });
        document.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrar(); });
    }

    // Nombre y rol del usuario en la cabecera del menú (si hay sesión iniciada)
    let usuario = null;
    try { usuario = JSON.parse(sessionStorage.getItem('usuario')); } catch (e) {}
    if (usuario) {
        const nombre = document.getElementById('menu-nombre');
        const rol = document.getElementById('menu-rol');
        if (nombre && usuario.nombre) nombre.textContent = usuario.nombre;
        if (rol && usuario.rol) rol.textContent = (usuario.rol === 'ADMIN') ? 'Administrador' : 'Estudiante';
    }
})();

// Misma lógica que index.html
function cerrarSesion() {
    sessionStorage.removeItem('usuario');
    window.location.href = 'index.html';
}
