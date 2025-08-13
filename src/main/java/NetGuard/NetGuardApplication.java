package NetGuard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication(scanBasePackages = {
		"NetGuard",
		"NetGuard.Login_Backend",
		"NetGuard.Dashboard_Features_Backend"  // CRITICAL: Added Dashboard backend scanning
})
@EnableScheduling
@Slf4j
public class NetGuardApplication {

	public static void main(String[] args) {
		SpringApplication.run(NetGuardApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		log.info("🚀 Initializing NetGuard Application...");

		// Create necessary directories
		createDirectoryIfNotExists("uploads");
		createDirectoryIfNotExists("uploads/profile-images");
		createDirectoryIfNotExists("logs");

		log.info("📁 Upload directories created successfully");
		log.info("🔐 JWT-based authentication enabled");
		log.info("📧 Password reset with OTP enabled");
		log.info("🖼️  Profile image upload enabled");
		log.info("🧹 Automatic token cleanup scheduled");
		log.info("🛡️  Dashboard Features Backend enabled");  // Added log for dashboard
		log.info("✅ NetGuard Application initialization completed!");

		// Show access URLs
		log.info("\n----------------------------------------------------------");
		log.info("\t🛡️  NetGuard Application is running! Access URLs:");
		log.info("\tLocal: \t\thttp://localhost:8080");
		log.info("\tExternal: \thttp://192.168.10.216:8080");
		log.info("\tAPI Base: \thttp://localhost:8080/api/auth");
		log.info("\tDashboard API: \thttp://localhost:8080/api/overview");  // Added dashboard API info
		log.info("\tProfile: \tdefault");
		log.info("----------------------------------------------------------");
	}

	private void createDirectoryIfNotExists(String dirPath) {
		File directory = new File(dirPath);
		if (!directory.exists()) {
			if (directory.mkdirs()) {
				log.debug("Directory created: {}", dirPath);
			} else {
				log.warn("Failed to create directory: {}", dirPath);
			}
		} else {
			log.debug("Directory already exists: {}", dirPath);
		}
	}
}