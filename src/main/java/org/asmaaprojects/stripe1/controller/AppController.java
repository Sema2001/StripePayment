package org.asmaaprojects.stripe1.controller;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asmaaprojects.stripe1.config.paypal.PaypalService;
import org.asmaaprojects.stripe1.model.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AppController {
    private final PaypalService paypalService; // Inject PaypalService

    @Value("${stripe.api.publicKey}")
    private String publicKey;
    @GetMapping("/")
    public String home(Model model){
        model.addAttribute("request", new Request());
        return "index";
    }

    @PostMapping("/")
    public String showCard(@ModelAttribute @Valid Request request,
                           BindingResult bindingResult,
                           Model model){
        if (bindingResult.hasErrors()){
            return "index";
        }
        model.addAttribute("publicKey", publicKey);
        model.addAttribute("amount", request.getAmount());
        model.addAttribute("email", request.getEmail());
        model.addAttribute("productName", request.getProductName());
        return "checkout";
    }


    @PostMapping("/payment/create")
    public RedirectView createPayment(){
        try{
            String cancelUrl = "https://localhost:8030/payment/cancel";
            String successUrl = "https://localhost:8030/payment/success";
            Payment payment = paypalService.createPayment(
                    10.0,
                    "USD",
                    "paypal",
                    "sale",
                    "Payment description",
                    cancelUrl,
                    successUrl
            );

            for(Links links :payment.getLinks()){
                if(links.getRel().equals("approval_url")){
                    return new RedirectView(links.getHref());
                }
            }

        } catch (PayPalRESTException e){
            log.error("Error occurred ::", e);
        }

        return new RedirectView("/payment/error");
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerId") String payerId
    ){

        try{

            Payment payment = paypalService.executePayment(paymentId,payerId);
            if (payment.getState().equals("approved")){
                return "paymentSuccess";
            }
        }catch (PayPalRESTException e){
            log.error("Error occurred ::", e);
        }
        return "paymentSuccess";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel(){
        return "paymentCancel";
    }

    @GetMapping("/payment/error")
    public String paymentError(){
        return "paymentError";
    }
}
