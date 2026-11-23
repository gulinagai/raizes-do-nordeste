package br.edu.uninter.raizes.api.controller;

import br.edu.uninter.raizes.api.dto.AtualizacaoStatusRequisicao;
import br.edu.uninter.raizes.api.dto.PedidoRequisicao;
import br.edu.uninter.raizes.api.dto.PedidoResposta;
import br.edu.uninter.raizes.api.dto.PedidosResposta;
import br.edu.uninter.raizes.api.erro.ErroResposta;
import br.edu.uninter.raizes.application.servico.AuditoriaServico;
import br.edu.uninter.raizes.application.servico.ItemSolicitado;
import br.edu.uninter.raizes.application.servico.PedidoServico;
import br.edu.uninter.raizes.domain.enumeracao.AcaoAuditoria;
import br.edu.uninter.raizes.infrastructure.seguranca.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Criação, consulta, andamento e cancelamento de pedidos.")
public class PedidoController {

    private final PedidoServico pedidoServico;
    private final AuditoriaServico auditoria;

    public PedidoController(PedidoServico pedidoServico, AuditoriaServico auditoria) {
        this.pedidoServico = pedidoServico;
        this.auditoria = auditoria;
    }

    @Operation(
            summary = "Listar pedidos",
            description = """
                    Qualquer perfil autenticado consulta. CLIENTE recebe somente os próprios pedidos, \
                    com status 200 e a lista filtrada. Os demais perfis veem todos.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de pedidos. A página vazia tem total de páginas zero."),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Página, limite, canal ou status inválidos. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public PedidosResposta listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Parameter(description = "Canal do pedido: APP, TOTEM, BALCAO, PICKUP ou WEB.", example = "APP")
            @RequestParam(name = "canalPedido", required = false) String canalPedido,
            @Parameter(
                    description = "Status: AGUARDANDO_PAGAMENTO, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO ou PAGAMENTO_RECUSADO.",
                    example = "EM_PREPARO")
            @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Página, a partir de 1.", example = "1")
            @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Itens por página, de 1 a 50.", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        PedidosResposta resposta = PedidosResposta.de(pedidoServico.listar(
                autenticado.id(), autenticado.perfil(), canalPedido, status, page, limit));
        auditoria.registrar(autenticado.id(), AcaoAuditoria.CONSULTAR_PEDIDOS, "pedido", null, "CONSULTA");
        return resposta;
    }

    @Operation(
            summary = "Cancelar pedido",
            description = """
                    Perfis ADMIN, GERENTE, ATENDENTE e CLIENTE. O cliente só cancela o próprio pedido. \
                    Permitido em AGUARDANDO_PAGAMENTO, EM_PREPARO e PRONTO. \
                    Antes do pagamento a reserva é liberada. Depois do pagamento o estoque físico volta. \
                    Pontos já resgatados retornam ao saldo.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Pedido cancelado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PedidoResposta.class),
                            examples = @ExampleObject(
                                    name = "Pedido cancelado",
                                    value = """
                                            {
                                              "id": 20,
                                              "unidadeId": 1,
                                              "clienteId": 5,
                                              "canalPedido": "APP",
                                              "status": "CANCELADO",
                                              "total": 34.90,
                                              "desconto": 0.00,
                                              "pontosResgatados": 0,
                                              "itens": [
                                                {
                                                  "produtoId": 1,
                                                  "quantidade": 1,
                                                  "precoUnitario": 34.90
                                                }
                                              ],
                                              "dataCriacao": "2026-11-16T18:00:00Z"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil sem permissão ou cliente cancelando pedido de outra pessoa. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Pedido não encontrado. O código do corpo é NAO_ENCONTRADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Pedido entregue, recusado ou já cancelado. O código do corpo é TRANSICAO_NAO_PERMITIDA.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Identificador inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/cancelamento")
    public PedidoResposta cancelar(
            @Parameter(description = "Identificador do pedido.", example = "20") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return PedidoResposta.de(pedidoServico.cancelar(id, autenticado.id(), autenticado.perfil()));
    }

    @Operation(
            summary = "Avançar status",
            description = """
                    Perfis ADMIN, GERENTE, COZINHA e ATENDENTE. \
                    COZINHA só pode informar PRONTO. ATENDENTE só pode informar ENTREGUE. \
                    ADMIN e GERENTE seguem a mesma sequência: EM_PREPARO para PRONTO e PRONTO para ENTREGUE. \
                    A entrega com consentimento ativo credita um ponto por real inteiro pago.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status avançado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PedidoResposta.class),
                            examples = @ExampleObject(
                                    name = "Pedido pronto",
                                    value = """
                                            {
                                              "id": 20,
                                              "unidadeId": 1,
                                              "clienteId": 5,
                                              "canalPedido": "APP",
                                              "status": "PRONTO",
                                              "total": 34.90,
                                              "desconto": 0.00,
                                              "pontosResgatados": 0,
                                              "itens": [
                                                {
                                                  "produtoId": 1,
                                                  "quantidade": 1,
                                                  "precoUnitario": 34.90
                                                }
                                              ],
                                              "dataCriacao": "2026-11-16T18:00:00Z"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil sem acesso à rota ou status que o perfil não pode pedir. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Pedido não encontrado. O código do corpo é NAO_ENCONTRADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Transição fora da sequência. O código do corpo é TRANSICAO_NAO_PERMITIDA.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Status ausente, valor inválido ou identificador inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/status")
    public PedidoResposta atualizarStatus(
            @Parameter(description = "Identificador do pedido.", example = "20") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody AtualizacaoStatusRequisicao requisicao) {
        return PedidoResposta.de(
                pedidoServico.atualizarStatus(id, requisicao.status(), autenticado.id(), autenticado.perfil()));
    }

    @Operation(
            summary = "Criar pedido",
            description = """
                    Perfis ADMIN, GERENTE, ATENDENTE e CLIENTE. O cliente só cria pedido para o próprio identificador. \
                    O pedido nasce em AGUARDANDO_PAGAMENTO e reserva a quantidade disponível, sem baixar o estoque físico.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Pedido criado.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PedidoResposta.class),
                            examples = @ExampleObject(
                                    name = "Pedido criado",
                                    value = """
                                            {
                                              "id": 20,
                                              "unidadeId": 1,
                                              "clienteId": 5,
                                              "canalPedido": "APP",
                                              "status": "AGUARDANDO_PAGAMENTO",
                                              "total": 34.90,
                                              "desconto": 0.00,
                                              "pontosResgatados": 0,
                                              "itens": [
                                                {
                                                  "produtoId": 1,
                                                  "quantidade": 1,
                                                  "precoUnitario": 34.90
                                                }
                                              ],
                                              "dataCriacao": "2026-11-16T18:00:00Z"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente ou inválido. O código do corpo é NAO_AUTENTICADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Perfil sem permissão ou cliente informando outro cliente. O código do corpo é ACESSO_NEGADO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unidade, cliente ou produto não encontrado. Cliente que não tem perfil CLIENTE também responde 404.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                            Unidade ou produto inativo, código RECURSO_INATIVO, \
                            ou quantidade acima do disponível, código ESTOQUE_INSUFICIENTE.""",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class))),
            @ApiResponse(
                    responseCode = "422",
                    description = "Campo obrigatório ausente, quantidade menor que 1 ou JSON inválido. O código do corpo é VALIDACAO.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResposta.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PedidoResposta> criar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody PedidoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResposta.de(pedidoServico.criar(
                autenticado.id(),
                autenticado.perfil(),
                requisicao.unidadeId(),
                requisicao.clienteId(),
                requisicao.canalPedido(),
                requisicao.itens().stream()
                        .map(item -> new ItemSolicitado(item.produtoId(), item.quantidade()))
                        .toList())));
    }
}
