package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.CardapioResposta;
import br.edu.uninter.raizes.application.servico.CardapioServico;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/unidades")
public class UnidadeController {

    private final CardapioServico cardapioServico;

    public UnidadeController(CardapioServico cardapioServico) {
        this.cardapioServico = cardapioServico;
    }

    @GetMapping("/{id}/cardapio")
    public CardapioResposta cardapio(
            @PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return CardapioResposta.de(cardapioServico.consultar(id, page, limit));
    }
}
