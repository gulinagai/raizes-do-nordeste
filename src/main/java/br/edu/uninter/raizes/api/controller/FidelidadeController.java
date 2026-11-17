package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.ConsentimentoRequisicao;
import br.edu.uninter.raizes.api.dto.FidelidadeResposta;
import br.edu.uninter.raizes.api.dto.ResgateRequisicao;
import br.edu.uninter.raizes.api.dto.ResgateResposta;
import br.edu.uninter.raizes.application.servico.FidelidadeServico;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fidelidade")
public class FidelidadeController {

    private final FidelidadeServico fidelidadeServico;

    public FidelidadeController(FidelidadeServico fidelidadeServico) {
        this.fidelidadeServico = fidelidadeServico;
    }

    @GetMapping
    public FidelidadeResposta consultar(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return FidelidadeResposta.de(fidelidadeServico.consultar(autenticado.id()));
    }

    @PutMapping
    public ResponseEntity<FidelidadeResposta> registrar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody ConsentimentoRequisicao requisicao) {
        return ResponseEntity.ok(FidelidadeResposta.de(
                fidelidadeServico.registrar(autenticado.id(), requisicao.consentimento())));
    }

    @PostMapping("/resgates")
    public ResponseEntity<ResgateResposta> resgatar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody ResgateRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResgateResposta.de(
                fidelidadeServico.resgatar(autenticado.id(), requisicao.pedidoId(), requisicao.pontos())));
    }
}
