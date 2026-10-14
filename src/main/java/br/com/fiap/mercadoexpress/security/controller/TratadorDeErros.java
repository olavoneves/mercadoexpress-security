package br.com.fiap.mercadoexpress.security.controller;

import br.com.fiap.mercadoexpress.security.exception.ProdutoNaoEncontradoException;
import br.com.fiap.mercadoexpress.security.exception.UsuarioNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Traduz as excecoes da aplicacao em paginas com a identidade visual do
 * projeto. Sem isso, um id inexistente na URL cairia na tela padrao do Boot.
 */
@ControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler({ProdutoNaoEncontradoException.class, UsuarioNaoEncontradoException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(RuntimeException erro, Model model) {
        model.addAttribute("mensagem", erro.getMessage());
        return "error/404";
    }
}
