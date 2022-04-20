package de.elb.wildfly.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import java.security.Principal;
import java.security.spec.AlgorithmParameterSpec;
import java.util.List;

public class FirebaseRealmIdentity implements RealmIdentity {

    FirebaseToken firebaseToken;
    private final Principal principal;

    public FirebaseRealmIdentity(Principal user) {
        this.principal = user;
    }

    @Override
    public Principal getRealmIdentityPrincipal() {
        return principal;
    }

    @Override
    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType,
                                                    String algorithmName, AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
        return SupportLevel.UNSUPPORTED;
    }

    @Override
    public <C extends Credential> C getCredential(Class<C> credentialType) throws RealmUnavailableException {
        return null;
    }

    @Override
    public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName)
            throws RealmUnavailableException {
        return PasswordGuessEvidence.class.isAssignableFrom(evidenceType) ? SupportLevel.SUPPORTED : SupportLevel.UNSUPPORTED;
    }

    @Override
    public boolean verifyEvidence(Evidence evidence) throws RealmUnavailableException {
        if (evidence instanceof PasswordGuessEvidence) {
            PasswordGuessEvidence guess = (PasswordGuessEvidence) evidence;
            try {
                firebaseToken = FirebaseAuth.getInstance().verifyIdToken(new String(guess.getGuess()));
                return true;
            } catch (FirebaseAuthException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean exists() throws RealmUnavailableException {
        return true;
    }

    @Override
    public Attributes getAttributes() throws RealmUnavailableException {

        MapAttributes map = new MapAttributes();

        if (firebaseToken.getClaims().containsKey("groups")) {
            map.addAll("Roles", (List) firebaseToken.getClaims().get("groups"));
        }
        if (firebaseToken.getClaims().containsKey("companies")) {
            map.addAll("companies", (List) firebaseToken.getClaims().get("companies"));
        }

        String picture = firebaseToken.getPicture();
        if (picture != null) {
            map.addFirst("picture", picture);
        }
        if (firebaseToken.getClaims().containsKey("user_id")) {
            map.addFirst("user_id", (String) firebaseToken.getClaims().get("user_id"));
        }
        map.addFirst("sub", firebaseToken.getUid());
        String email = firebaseToken.getEmail();
        if (email != null) {
            map.addFirst("email", email);
        }
        String name = firebaseToken.getName();
        if (name != null) {
            map.addFirst("name", name);
        }

        return map;
    }

    @Override
    public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
        return AuthorizationIdentity.basicIdentity(getAttributes());
    }
}