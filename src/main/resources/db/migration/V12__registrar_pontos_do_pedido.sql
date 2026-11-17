ALTER TABLE pedido ADD COLUMN pontos_resgatados INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pedido ADD COLUMN desconto NUMERIC(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE pedido ADD COLUMN pontos_creditados BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pedido ADD COLUMN resgate_estornado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pedido ADD CONSTRAINT ck_pedido_pontos_resgatados CHECK (pontos_resgatados >= 0);
ALTER TABLE pedido ADD CONSTRAINT ck_pedido_desconto CHECK (desconto >= 0);

ALTER TABLE registro_auditoria DROP CONSTRAINT ck_auditoria_acao;

ALTER TABLE registro_auditoria ADD CONSTRAINT ck_auditoria_acao
    CHECK (acao IN (
        'CRIAR_PEDIDO',
        'CANCELAR_PEDIDO',
        'ATUALIZAR_STATUS_PEDIDO',
        'PROCESSAR_PAGAMENTO',
        'MOVIMENTAR_ESTOQUE',
        'CONSULTAR_PEDIDOS',
        'CONSULTAR_USUARIO',
        'CADASTRAR_USUARIO',
        'AUTENTICAR',
        'REGISTRAR_CONSENTIMENTO',
        'CREDITAR_PONTOS',
        'RESGATAR_PONTOS',
        'DEVOLVER_PONTOS'
    ));
