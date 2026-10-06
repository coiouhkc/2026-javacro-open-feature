///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.springframework.boot:spring-boot-starter-web:3.5.3

package me.abratuhi.demo.openfeature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
public class demo1 {

	private static final String HAWAII = "Hawaii";

	private static final String FEATURE_PIZZA_HAWAII = "list-pizza-hawaii";

	private final Map<String, Boolean> featureState = new HashMap<>(Map.of(FEATURE_PIZZA_HAWAII, Boolean.FALSE));

	public static void main(String[] args) {
		SpringApplication.run(demo1.class, args);
	}

	@RestController
	public class PizzaController {

		private static final String HAWAII = "Hawaii";

		private static final Set<Pizza> PIZZAS = Set.of(new Pizza("Margherita"), new Pizza("Marinara"),
				new Pizza("Capricciosa"),
				new Pizza("Quattro Formaggi"), new Pizza(HAWAII));

		@GetMapping("/pizza")
		public ResponseEntity<Pizzas> getPizzas() {
			ArrayList<Pizza> pizzas = new ArrayList<>(PIZZAS);
			if (!featureState.get(FEATURE_PIZZA_HAWAII)) {
				pizzas.removeIf(pizza -> pizza.name().equals(HAWAII));
			}
			return ResponseEntity.ok(new Pizzas(pizzas));
		}

		@PostMapping("/pizza")
		public ResponseEntity<Pizza> orderPizza(@RequestBody Pizza pizza) {
			if (!PIZZAS.contains(pizza)) {
				return ResponseEntity.notFound().build();
			}
			if (pizza.name().equals(HAWAII)
					&& !featureState.get(FEATURE_PIZZA_HAWAII)) {
				return ResponseEntity.badRequest().body(new Pizza("Very bad taste!"));
			}
			return ResponseEntity.ok(pizza);
		}

		public record Pizzas(List<Pizza> pizzas) {
		}

		public record Pizza(String name) {
		}
	}

	@RestController
	public class FeatureController {

		@GetMapping("/feature")
		public ResponseEntity<Features> getFeatures() {
			return ResponseEntity.ok(new Features(featureState.entrySet()
				.stream()
				.map(kv -> new Feature(kv.getKey(), kv.getValue()))
				.collect(Collectors.toSet())));
		}

		@PutMapping("/feature")
		public ResponseEntity<Features> setFeature(@RequestBody Feature feature) {
			if (featureState.containsKey(feature.name())) {
				featureState.put(feature.name(), feature.state());
			}
			return ResponseEntity.ok(new Features(featureState.entrySet()
				.stream()
				.map(kv -> new Feature(kv.getKey(), kv.getValue()))
				.collect(Collectors.toSet())));
		}

		public record Features(Set<Feature> features) {
		}

		public record Feature(String name, boolean state) {
		}
	}

}