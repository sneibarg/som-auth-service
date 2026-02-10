package org.springy.som.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springy.som.client.model.CharacterView;
import org.springy.som.client.model.PlayerView;
import org.springy.som.security.KeycloakTokenClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SomApiClientTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private KeycloakTokenClient tokenClient;

    @Mock
    private RestClient.RequestHeadersUriSpec playersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec playersHeadersSpec;

    @Mock
    private RestClient.ResponseSpec playersResponseSpec;

    @Mock
    private RestClient.RequestHeadersUriSpec charactersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec charactersHeadersSpec;

    @Mock
    private RestClient.ResponseSpec charactersResponseSpec;

    @Test
    @SuppressWarnings("unchecked")
    void getAccountByName_returnsEnrichedPlayer() throws Exception {
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.get()).thenReturn(playersUriSpec, charactersUriSpec);

        when(playersUriSpec.uri(anyString())).thenReturn(playersHeadersSpec);
        when(playersHeadersSpec.headers(any())).thenAnswer(invocation -> playersHeadersSpec);
        when(playersHeadersSpec.retrieve()).thenReturn(playersResponseSpec);

        when(charactersUriSpec.uri(anyString())).thenReturn(charactersHeadersSpec);
        when(charactersHeadersSpec.headers(any())).thenAnswer(invocation -> charactersHeadersSpec);
        when(charactersHeadersSpec.retrieve()).thenReturn(charactersResponseSpec);

        when(tokenClient.getAccessToken()).thenReturn("token");

        Object remotePlayer = newRemotePlayerView(
                "acct-1",
                "Alice",
                "Wonder",
                "alice",
                "alice@example.com",
                "secret",
                List.of("c1")
        );
        when(playersResponseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(remotePlayer));

        CharacterView character = new CharacterView(
                "c1",
                "acct-1",
                "AliceChar",
                "title",
                "desc",
                "race",
                "sex",
                "class",
                "room",
                "area",
                "guild",
                "role",
                false,
                List.of(),
                10,
                5,
                7,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
        when(charactersResponseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(character));

        SomApiClient client = new SomApiClient(restClientBuilder, tokenClient, "http://modulith");
        PlayerView result = client.getAccountByName("alice");

        assertThat(result).isNotNull();
        assertThat(result.accountName()).isEqualTo("alice");
        assertThat(result.playerCharacterList()).hasSize(1);
        assertThat(result.playerCharacterList().get(0).id()).isEqualTo("c1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAccountByName_returnsNullWhenMissing() throws Exception {
        when(restClientBuilder.build()).thenReturn(restClient);
        when(restClient.get()).thenReturn(playersUriSpec);

        when(playersUriSpec.uri(anyString())).thenReturn(playersHeadersSpec);
        when(playersHeadersSpec.headers(any())).thenAnswer(invocation -> playersHeadersSpec);
        when(playersHeadersSpec.retrieve()).thenReturn(playersResponseSpec);

        when(tokenClient.getAccessToken()).thenReturn("token");
        when(playersResponseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of());

        SomApiClient client = new SomApiClient(restClientBuilder, tokenClient, "http://modulith");
        PlayerView result = client.getAccountByName("bob");

        assertThat(result).isNull();
    }

    private static Object newRemotePlayerView(String id,
                                              String firstName,
                                              String lastName,
                                              String accountName,
                                              String emailAddress,
                                              String password,
                                              List<String> playerCharacterList) throws Exception {
        Class<?> clazz = Class.forName("org.springy.som.client.SomApiClient$RemotePlayerView");
        Constructor<?> ctor = clazz.getDeclaredConstructor(
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                List.class
        );
        ctor.setAccessible(true);
        return ctor.newInstance(id, firstName, lastName, accountName, emailAddress, password, playerCharacterList);
    }
}
