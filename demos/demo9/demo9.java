///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.springframework.boot:spring-boot-starter-web:3.5.3
//DEPS dev.openfeature:sdk:1.20.1
//DEPS dev.openfeature.contrib.providers:unleash:0.1.3-alpha

package demos.demo9;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.openfeature.contrib.providers.unleash.UnleashProvider;
import dev.openfeature.contrib.providers.unleash.UnleashProviderConfig;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;
import io.getunleash.util.UnleashConfig;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class demo9 {

	private static final String FEATURE_PIZZA_HAWAII = "list-pizza-hawaii";

	private Client client;

	public static void main(String[] args) {
		SpringApplication.run(demo9.class, args);
	}

	@PostConstruct
	public void initOpenFeatureClient() {
		OpenFeatureAPI api = OpenFeatureAPI.getInstance();
		try {
			api.setProviderAndWait(new UnleashProvider(UnleashProviderConfig.builder()
				.unleashConfigBuilder(UnleashConfig.builder()
					.appName("unleash-onboarding-java")
					.instanceId("unleash-onboarding-instance")
					.unleashAPI("http://localhost:4242/api/")
					.apiKey("default:development.unleash-insecure-api-token"))
				.build()));
		} catch (Exception e) {
			// handle initialization failure
			e.printStackTrace();
		}

		// create a client
		this.client = api.getClient();
	}

	@RestController
	public class PizzaController {

		private static final String HAWAII = "Hawaii";

		private static final Set<Pizza> PIZZAS = Set.of(new Pizza("Margherita"), new Pizza("Marinara"),
				new Pizza("Capricciosa"),
				new Pizza("Quattro Formaggi"), new Pizza(HAWAII));

		@GetMapping("/pizza")
		public ResponseEntity<Pizzas> getPizzas(
				@RequestHeader(value = "x-pizzeria-location", required = false) String location) {
			ArrayList<Pizza> pizzas = new ArrayList<>(PIZZAS);
			if (!client.getBooleanValue(FEATURE_PIZZA_HAWAII, Boolean.FALSE,
					new ImmutableContext(Map.of("location", new Value(location))))) {
				pizzas.removeIf(pizza -> pizza.name().equals(HAWAII));
			}
			return ResponseEntity.ok(new Pizzas(pizzas));
		}

		@PostMapping("/pizza")
		public ResponseEntity<Pizza> orderPizza(
				@RequestHeader(value = "x-pizzeria-location", required = false) String location,
				@RequestBody Pizza pizza) {
			if (!PIZZAS.contains(pizza)) {
				return ResponseEntity.notFound().build();
			}
			if (pizza.name().equals(HAWAII) && !client.getBooleanValue(FEATURE_PIZZA_HAWAII, Boolean.FALSE,
					new ImmutableContext(Map.of("location", new Value(location))))) {
				return ResponseEntity.badRequest().body(new Pizza("Very bad taste!"));
			}
			return ResponseEntity.ok(pizza);
		}

		public record Pizzas(List<Pizza> pizzas) {
		}

		public record Pizza(String name) {
		}
	}
}