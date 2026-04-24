# Suwinet plugin

For retrieving data from Suwinet (https://www.bkwi.nl/producten/suwinet-services/).

## Capabilities

This plugin can access Suwinet soap webservices, and process the response.

## Example application

This project contains a working example application which is meant to showcase the slack plugin.
It will send a request to a soap webservice to retrieve BRP data, which is one of the existing data sources of Siwunet.
Other resources are Kadaster, DUO, RWD, UWV, SVB...

### Suwinet service Simulator

The example will connect to a backend soap service. For test purposes we created a Suwinet Simulator that will have the
same contract as the 'Real' suwinet services.
You can find the repository at https://github.com/generiekzaakafhandelcomponent/suwinet-simulator. It is easy to install
and already includes example data.

#### Test BSN numbers

The simulator ships with pre-configured response data. The following BSN numbers can be used for testing:

| BSN         | Naam           | Partner                      | Kinderen                                           | Bijzonderheden                                                                      |
|-------------|----------------|------------------------------|----------------------------------------------------|-------------------------------------------------------------------------------------|
| `111111110` | Henk de Jansen | Sierra Kooyman (`999991954`) | 4 kinderen (`243000157`, `243000017`, `010002546`) | Meest complete dataset, ideaal voor het testen van het volledige BRP-ophalen proces |
| `444444440` |                |                              |                                                    | BRP, Kadaster, DUO, RDW, SVB, UWV                                                   |
| `999991954` | Sierra Kooyman |                              |                                                    | Partner van 111111110                                                               |
| `022264541` |                |                              |                                                    | BRP, Bijstandsregelingen, DUO, Kadaster, RDW, UWV                                   |
| `689735273` |                |                              |                                                    | BRP, Bijstandsregelingen, DUO, Kadaster, RDW, SVB, UWV                              |
| `999993549` |                |                              |                                                    | BRP, Bijstandsregelingen, DUO, Kadaster, RDW, SVB, UWV                              |
| `999998493` |                |                              |                                                    | BRP, Bijstandsregelingen, DUO, Kadaster, RDW, SVB, UWV                              |
| `999991322` |                |                              |                                                    | BRP, Bijstandsregelingen, RDW                                                       |
| `123456782` |                |                              |                                                    | BRP, RDW                                                                            |
| `347910099` |                |                              |                                                    | BRP, Kadaster, RDW, UWV                                                             |

BSN `111111110` wordt aanbevolen voor het testen van het `suwinet-brp-ophalen` proces omdat het een partner en kinderen
bevat, waardoor alle drie de plugin acties (persoonsgegevens, partner, kinderen) worden doorlopen.

### Running the example application

#### Start docker

Make sure docker is running. Then use the following commands:

```shell
cd suwinet
docker compose up
```

#### Start frontend

```shell
cd suwinet/frontend
npm install
npm run start
```

#### Start backend

By gradle script:

`Plugins -> suwinet -> backend -> app -> Tasks -> application -> bootRun`

Or use commend line:

```shell
brew install gradle

cd suwinet/backend/app
gralde bootRun
```

#### Keycloak users

The example application has a few test users that are preconfigured.

| Name         | Role           | Username  | Password  |
|--------------|----------------|-----------|-----------|
| James Vance  | ROLE_USER      | user      | user      |
| Asha Miller  | ROLE_ADMIN     | admin     | admin     |
| Morgan Finch | ROLE_DEVELOPER | developer | developer |

## Source code

The source code is split up into 2 modules:

1. [Frontend](./frontend)
2. [Backend](./backend)
