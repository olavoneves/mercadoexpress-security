/*
 * Unico script da aplicacao: pede confirmacao antes de enviar um formulario
 * marcado com data-confirmar (ex.: promover usuario a administrador).
 *
 * E melhoria progressiva: com o JavaScript desligado o botao continua
 * funcionando, so nao exibe o aviso. A exclusao de produto nem depende dele:
 * tem uma tela de confirmacao propria.
 */
document.addEventListener('submit', function (evento) {
    var formulario = evento.target.closest('[data-confirmar]');
    if (formulario && !window.confirm(formulario.dataset.confirmar)) {
        evento.preventDefault();
    }
});
