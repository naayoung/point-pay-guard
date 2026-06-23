package com.pointpay.guard.config;

import com.pointpay.guard.domain.user.PointUser;
import com.pointpay.guard.domain.wallet.Wallet;
import com.pointpay.guard.repository.UserRepository;
import com.pointpay.guard.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDemoUser(DemoDataSeeder demoDataSeeder) {
        return args -> demoDataSeeder.seed();
    }

    @Configuration
    static class DemoDataSeeder {

        private final UserRepository userRepository;
        private final WalletRepository walletRepository;

        DemoDataSeeder(UserRepository userRepository, WalletRepository walletRepository) {
            this.userRepository = userRepository;
            this.walletRepository = walletRepository;
        }

        @Transactional
        public void seed() {
            if (userRepository.count() > 0) {
                return;
            }
            PointUser user = userRepository.save(new PointUser("demo-user"));
            walletRepository.save(new Wallet(user, 100_000L));
        }
    }
}
