package ml.hfer.binance.pumping.controller;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pump")
public class PumpController {

    @RequestMapping("/start")
    @ResponseBody
    public String pump( @RequestParam(required = false) MultiValueMap<?, ?> paramMap) {
        return "done";
    }
}
