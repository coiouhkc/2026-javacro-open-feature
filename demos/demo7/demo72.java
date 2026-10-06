///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.springframework.boot:spring-boot-starter-web:3.5.3
//DEPS dev.openfeature:sdk:1.20.1
//DEPS dev.openfeature.contrib.providers:flagd:0.11.19
//RUNTIME_OPTIONS -Dserver.port=8081

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
public class demo72 {

	private static final String FEATURE_PIZZA_HAWAII = "list-pizza-hawaii";

	private static final String FEATURE_PIZZA_DIAVOLO = "list-pizza-diavolo";

	private Client client;

	public static void main(String[] args) {
		SpringApplication.run(demo72.class, args);
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
	public class RecipeController {

		@org.springframework.beans.factory.annotation.Value("${app.env:local}")
		private String env;

		private static final String HAWAII = "Hawaii";

		private static final String DIAVOLO = "Diavolo";

		private static final List<Recipe> RECIPES = List.of(new Recipe("Margherita", "Tomatoes, mozzarella, basil."),
				new Recipe("Marinara", "Tomato sauce, olive oil, oregano, garlic."),
				new Recipe("Capricciosa", "Ham, mushrooms, artichokes, egg."),
				new Recipe("Quattro Formaggi",
						"Mozzarella, Gorgonzola and two others [cheese] depending on the region."),
				new Recipe(HAWAII, " Pineapple, tomato sauce, mozzarella cheese, and either ham or bacon."), new Recipe(
						DIAVOLO, "Tomatoes, mozzarella, spicy salami, pork sausage, calamata olives, chili flakes."));

		@GetMapping("/recipe")
		public ResponseEntity<Recipes> getRecipes(
				@RequestHeader(value = "x-pizzeria-location", required = false) String location) {
			ArrayList<Recipe> recipes = new ArrayList<>(RECIPES);
			if (!client.getBooleanValue(FEATURE_PIZZA_HAWAII, Boolean.FALSE,
					new ImmutableContext(Map.of("location", new Value(location), "env", new Value(env))))) {
				recipes.removeIf(recipe -> recipe.name().equals(HAWAII));
			}
			if (!client.getBooleanValue(FEATURE_PIZZA_DIAVOLO, Boolean.FALSE)) {
				recipes.removeIf(recipe -> recipe.name().equals(DIAVOLO));
			}
			return ResponseEntity.ok(new Recipes(recipes));
		}

		public record Recipes(List<Recipe> recipes) {
		}

		public record Recipe(String name, String text) {
		}
	}
}