package ml.hfer.binance.pumping.controller;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import ml.hfer.binance.pumping.constant.SideENUM;
import ml.hfer.binance.pumping.pojo.OrderBook;
import ml.hfer.binance.pumping.pojo.PumpReq;
import ml.hfer.binance.pumping.pojo.RetMsg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

@RestController
@RequestMapping("/pump")
public class PumpController {

    @Value("${api.key}")
    private String realApiKey = "none";

    @Value("${api.secret}")
    private String realApiSecret = "none";

    private String ip = "127.0.0.1";
    private int port = 1080;

    private String btcBalance = "0.01669739";

    /**
     * @param
     * @return
     */
    @RequestMapping("/start")
    @ResponseBody
    public String pump(PumpReq req) {

        String symbol = req.getBase() + req.getQuote();

//        List<Integer> precision = getPrecision(symbol);
        //真实市场价
        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=15m&limit=3";
        String rs = proxyGet(url);
        JSONArray jsonArray = JSONUtil.parseArray(rs);
        List firstData = (List) jsonArray.get(0);
        String priceOfStart = (String) firstData.get(1);//开盘价
        String volume = (String) firstData.get(5);//成交量


        BigDecimal priceOfStripZeros = new BigDecimal(priceOfStart).stripTrailingZeros();
        BigDecimal buyPrice = priceOfStripZeros.multiply(new BigDecimal(req.getMulti())).setScale(priceOfStripZeros.scale(), BigDecimal.ROUND_HALF_UP);

        BigDecimal volumeOfStripZeros = new BigDecimal(volume).stripTrailingZeros();
        BigDecimal quantity = new BigDecimal(btcBalance).divide(buyPrice, volumeOfStripZeros.scale(), BigDecimal.ROUND_DOWN);

        String newOrderRet = null;
        boolean stop = false;
        while (!stop) {
            newOrderRet = newOrder(symbol, buyPrice, quantity, SideENUM.SELL);
            RetMsg retMsg = JSONUtil.toBean(newOrderRet, RetMsg.class);
            if (retMsg.getCode() != 0) {
                System.out.println("1.请求失败：" + retMsg);
                if (retMsg.getCode() == 429) {
                    return "失败请求已超限";
                }
            }
            if (newOrderRet.contains("my_order_id_buy")) {
                stop = true;
            }
        }
        System.out.println("买入" + symbol + ":\n响应:" + newOrderRet);
        //下单结果判断
        boolean buyIs = newOrderRet.contains("my_order_id_buy");
        if (buyIs) {//下单成功
            BigDecimal sellPrice = priceOfStripZeros.multiply(new BigDecimal("3")).setScale(priceOfStripZeros.scale(), BigDecimal.ROUND_HALF_UP);
            String sellRes = newOrder(symbol, sellPrice, quantity, SideENUM.SELL);
        } else {//下单失败
            String newOrderRet2 = newOrder(symbol, buyPrice, quantity, SideENUM.SELL);
            System.out.println("买入" + symbol + ":\n响应:" + newOrderRet2);
        }


        return "done";
    }

    private List<Integer> getPrecision(String symbol) {
        String url = "https://api.binance.com/api/v3/ticker/bookTicker?symbol=DLTBTC";
        String rs = proxyGet(url);
        OrderBook orderBook = JSONUtil.toBean(rs, OrderBook.class);
        return null;
    }


    String newOrder(String symbol, BigDecimal price, BigDecimal quantity, SideENUM side) {
        //真实市场价
        //symbol,side,price,
        long timestamp = System.currentTimeMillis();
        byte[] apikeyOnBytes = realApiSecret.getBytes();
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, apikeyOnBytes);
        String endPoint = "https://api.binance.com/api/v3/order?";
        String uri = "symbol=" + symbol +
                "&side=" + side + "&type=LIMIT&timeInForce=GTC&" +
                "quantity=" + quantity + "&" +
                "price=" + price + "&" +
                "newClientOrderId=my_order_id_buy&" +
                "newOrderRespType=ACK&" +
                "timestamp=" + timestamp;
        String allUrl = endPoint + uri + "&signature=" + hMac.digestHex(uri);
        String body = proxyPost(allUrl);
        return body;
    }


    String proxyGet(String url) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        String body = HttpRequest.get(url).setProxy(proxy).execute().body();
        return body;
    }

    String proxyPost(String url) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        String body = HttpRequest.post(url).header("X-MBX-APIKEY", realApiKey).setProxy(proxy).execute().body();
        return body;
    }
}
