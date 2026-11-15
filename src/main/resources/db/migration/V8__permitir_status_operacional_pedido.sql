ALTER TABLE pedido DROP CONSTRAINT ck_pedido_status;

ALTER TABLE pedido ADD CONSTRAINT ck_pedido_status
    CHECK (status IN (
        'AGUARDANDO_PAGAMENTO',
        'EM_PREPARO',
        'PRONTO',
        'ENTREGUE',
        'PAGAMENTO_RECUSADO'
    ));
