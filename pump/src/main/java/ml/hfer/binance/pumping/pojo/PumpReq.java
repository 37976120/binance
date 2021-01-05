package ml.hfer.binance.pumping.pojo;


import lombok.Data;
import ml.hfer.binance.pumping.constant.NewOrderRespTypeEnum;
import ml.hfer.binance.pumping.constant.SideENUM;
import ml.hfer.binance.pumping.constant.StopLimitTimeInForceEnum;

import java.math.BigDecimal;

@Data
public class PumpReq {

    private String symbol;

    private String listClientOrderId;

    private SideENUM side;

    private BigDecimal quantity;

    private String limitClientOrderId;

    private BigDecimal price;

    private BigDecimal limitIcebergQty;

    private String stopClientOrderId;

    private BigDecimal stopPrice;

    private BigDecimal stopLimitPrice;

    private BigDecimal stopIcebergQty;

    private StopLimitTimeInForceEnum stopLimitTimeInForce;

    private NewOrderRespTypeEnum newOrderRespType;

    private Long recvWindow;

    private Long timestamp;
}
