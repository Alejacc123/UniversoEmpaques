/*
 * Buscador rapido para las tablas (sin recargar la pagina).
 * Uso en la vista:
 *   <input type="search" data-filtro-tabla="idDeLaTabla" placeholder="Buscar...">
 *   <table id="idDeLaTabla"> ... </table>
 * Oculta las filas del <tbody> que no contengan el texto escrito.
 */
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-filtro-tabla]').forEach(function (input) {
        var tabla = document.getElementById(input.getAttribute('data-filtro-tabla'));
        if (!tabla) return;
        var quitarTildes = function (t) {
            return t.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
        };
        input.addEventListener('input', function () {
            var buscado = quitarTildes(input.value.trim());
            tabla.querySelectorAll('tbody tr').forEach(function (fila) {
                if (fila.hasAttribute('data-sin-datos')) return;
                fila.style.display = quitarTildes(fila.textContent).indexOf(buscado) >= 0 ? '' : 'none';
            });
        });
    });
});
