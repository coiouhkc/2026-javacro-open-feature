///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.springframework.boot:spring-boot-starter-web:3.5.3
//DEPS dev.openfeature:sdk:1.20.1
//DEPS dev.openfeature.contrib.providers:flagd:0.11.19

package me.abratuhi.demo.openfeature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.openfeature.contrib.providers.flagd.Config;
import dev.openfeature.contrib.providers.flagd.FlagdOptions;
import dev.openfeature.contrib.providers.flagd.FlagdProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.providers.memory.InMemoryProvider;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class demo71 {

	private static final String FEATURE_PIZZA_HAWAII = "list-pizza-hawaii";

	private static final String FEATURE_PIZZA_DIAVOLO = "list-pizza-diavolo";

	private Client client;

	public static void main(String[] args) {
		SpringApplication.run(demo71.class, args);
	}

	@PostConstruct
	public void initOpenFeatureClient() {
		OpenFeatureAPI api = OpenFeatureAPI.getInstance();
		try {
			api.setProviderAndWait(new FlagdProvider(
					FlagdOptions.builder()
						.resolverType(Config.Resolver.RPC)
						.host("localhost")
						.port(8013)
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

		@org.springframework.beans.factory.annotation.Value("${app.env:local}")
		private String env;

		private static final String HAWAII = "Hawaii";

		private static final String DIAVOLO = "Diavolo";

		private static final Set<Pizza> PIZZAS = Set.of(new Pizza("Margherita"), new Pizza("Marinara"),
				new Pizza("Capricciosa"),
				new Pizza("Quattro Formaggi"), new Pizza(HAWAII), new Pizza(DIAVOLO));

		@GetMapping("/pizza")
		public ResponseEntity<Pizzas> getPizzas(
				@RequestHeader(value = "x-pizzeria-location", required = false) String location) {
			ArrayList<Pizza> pizzas = new ArrayList<>(PIZZAS);
			if (!client.getBooleanValue(FEATURE_PIZZA_HAWAII, Boolean.FALSE,
					new ImmutableContext(Map.of("location", new Value(location), "env", new Value(env))))) {
				pizzas.removeIf(pizza -> pizza.name().equals(HAWAII));
			}
			if (!client.getBooleanValue(FEATURE_PIZZA_DIAVOLO, Boolean.FALSE)) {
				pizzas.removeIf(pizza -> pizza.name().equals(DIAVOLO));
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
					new ImmutableContext(Map.of("location", new Value(location), "env", new Value(env))))) {
				return ResponseEntity.badRequest().body(new Pizza("Very bad taste!"));
			}
			if (pizza.name.equals(DIAVOLO) && !client.getBooleanValue(FEATURE_PIZZA_DIAVOLO, Boolean.FALSE)) {
				return ResponseEntity.badRequest().body(new Pizza("Sorry, mate, better luck next time."));
			}
			return ResponseEntity.ok(pizza);
		}

		public record Pizzas(List<Pizza> pizzas) {
		}

		public record Pizza(String name) {
		}
	}
}