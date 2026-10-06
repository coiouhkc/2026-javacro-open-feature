///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.springframework.boot:spring-boot-starter-web:3.5.3

package me.abratuhi.demo.openfeature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

@SpringBootApplication
public class demo2 {

	private FeaturesMBean featureManager = new Features();

	public static void main(String[] args) {
		SpringApplication.run(demo2.class, args);
	}

	@PostConstruct
	public void exposeFeatureManager() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName("me.abratuhin.demo.openfeature:type=Features");
		mbs.registerMBean(featureManager, name);
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
			if (!featureManager.getFeatureState(FeaturesMBean.FEATURE_PIZZA_HAWAII)) {
				pizzas.removeIf(pizza -> pizza.name().equals(HAWAII));
			}
			return ResponseEntity.ok(new Pizzas(pizzas));
		}

		@PostMapping("/pizza")
		public ResponseEntity<Pizza> orderPizza(@RequestBody Pizza pizza) {
			if (!PIZZAS.contains(pizza)) {
				return ResponseEntity.notFound().build();
			}
			if (pizza.name().equals(HAWAII) && !featureManager.getFeatureState(FeaturesMBean.FEATURE_PIZZA_HAWAII)) {
				return ResponseEntity.badRequest().body(new Pizza("Very bad taste!"));
			}
			return ResponseEntity.ok(pizza);
		}

		public record Pizzas(List<Pizza> pizzas) {
		}

		public record Pizza(String name) {
		}
	}

	public interface FeaturesMBean {
		public static final String FEATURE_PIZZA_HAWAII = "list-pizza-hawaii";

		boolean getFeatureState(String featureName);

		void setFeatureState(String featureName, boolean featureValue);
	}

	public class Features implements FeaturesMBean {

		private final Map<String, Boolean> featureState = new HashMap<>(Map.of(FEATURE_PIZZA_HAWAII, Boolean.FALSE));

		@Override
		public boolean getFeatureState(String featureName) {
			return featureState.getOrDefault(featureName, false);
		}

		@Override
		public void setFeatureState(String featureName, boolean featureValue) {
			if (featureState.containsKey(featureName)) {
				featureState.put(featureName, featureValue);
			}
		}

	}
}