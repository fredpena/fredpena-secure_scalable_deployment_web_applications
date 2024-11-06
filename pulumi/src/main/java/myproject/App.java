package myproject;

import com.pulumi.Pulumi;
import com.pulumi.digitalocean.Droplet;
import com.pulumi.digitalocean.DropletArgs;
import io.github.cdimascio.dotenv.Dotenv;

public class App {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String fingerprint = dotenv.get("FINGERPRINT");

        Pulumi.run(ctx -> {
            // Create a new Droplet in nyc1
            Droplet droplet = new Droplet("web", DropletArgs.builder()
                    .image("debian-12-x64")
                    .name("iac-madrid-jug")
                    .region("lon1")
                    .size("s-1vcpu-2gb")
                    .sshKeys(fingerprint)
                    .build());

            System.out.println("The public IP address of your Droplet application");
            System.out.println("IP  => " + droplet.ipv4Address());
        });
    }
}
