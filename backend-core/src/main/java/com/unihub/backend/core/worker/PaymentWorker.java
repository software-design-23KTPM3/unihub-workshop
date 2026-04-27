// package com.unihub.backend.core.worker;

// import com.unihub.backend.core.model.entity.Registration;
// import com.unihub.backend.core.model.entity.Transaction;
// import com.unihub.backend.core.model.enums.RegistrationStatus;
// import com.unihub.backend.core.model.enums.TransactionStatus;
// import com.unihub.backend.core.repository.RegistrationRepository;
// import com.unihub.backend.core.repository.TransactionRepository;
// import com.unihub.backend.core.service.PaymentGatewayService;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;

// @Component
// public class PaymentWorker {

//     private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentWorker.class);

//     private final TransactionRepository transactionRepository;
//     private final RegistrationRepository registrationRepository;
//     private final PaymentGatewayService paymentGatewayService;

//     public PaymentWorker(TransactionRepository transactionRepository, 
//                          RegistrationRepository registrationRepository, 
//                          PaymentGatewayService paymentGatewayService) {
//         this.transactionRepository = transactionRepository;
//         this.registrationRepository = registrationRepository;
//         this.paymentGatewayService = paymentGatewayService;
//     }

//     @RabbitListener(queues = "payment.queue")
//     @Transactional
//     public void handlePayment(com.unihub.backend.core.model.dto.RegistrationRequest request) {
//         log.info("Starting payment processing for registration {}", request.getIdempotencyKey());

//         if (transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
//             log.info("Transaction already exists for idempotency key {}", request.getIdempotencyKey());
//             return;
//         }

//         Registration registration = registrationRepository.findByIdempotencyKey(request.getIdempotencyKey())
//                 .orElse(null);

//         if (registration == null) {
//             log.warn("Registration not found for key {}", request.getIdempotencyKey());
//             return;
//         }

//         Transaction transaction = Transaction.builder()
//                 .registration(registration)
//                 .amount(registration.getWorkshop().getPrice())
//                 .status(TransactionStatus.PENDING)
//                 .idempotencyKey(request.getIdempotencyKey())
//                 .build();

//         transaction = transactionRepository.save(transaction);

//         try {
//             String pgId = paymentGatewayService.processPayment(transaction.getId(), transaction.getAmount().doubleValue());
            
//             if ("PENDING_RETRY".equals(pgId)) {
//                 transaction.setStatus(TransactionStatus.PENDING);
//             } else {
//                 transaction.setStatus(TransactionStatus.SUCCESS);
//                 transaction.setPgTransactionId(pgId);
//                 registration.setStatus(RegistrationStatus.SUCCESS);
//             }
//         } catch (Exception e) {
//             transaction.setStatus(TransactionStatus.FAILED);
//             registration.setStatus(RegistrationStatus.FAILED);
//         }

//         transactionRepository.save(transaction);
//         registrationRepository.save(registration);
//     }
// }
