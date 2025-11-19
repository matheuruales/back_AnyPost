# Configuración de Variables de Entorno

Este proyecto utiliza variables de entorno para manejar información sensible como API keys, contraseñas y secretos. **NUNCA subas archivos `.env` al repositorio**.

## Configuración Local

1. Crea un archivo `.env` en la raíz del proyecto (junto a `pom.xml`)
2. Copia las siguientes variables y reemplaza los valores con tus credenciales reales:

```properties
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/railway
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=tu-password-de-base-de-datos

# Azure Blob Storage
AZURE_BLOB_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=anypost;AccountKey=tu-account-key;EndpointSuffix=core.windows.net
AZURE_BLOB_CONTAINER_NAME=uploads
AZURE_BLOB_PUBLIC=true

# OpenAI API
OPENAI_API_KEY=sk-proj-tu-openai-api-key
OPENAI_API_BASE_URL=https://api.openai.com/v1
OPENAI_IMAGES_MODEL=dall-e-3

# JWT Security
APPLICATION_SECURITY_JWT_SECRET=tu-very-long-secret-key-for-jwt-token-generation
APPLICATION_SECURITY_JWT_EXPIRATION=3600000

# Mail Configuration (Google SMTP)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=tu-email@gmail.com
SPRING_MAIL_PASSWORD=tu-app-password

# External Services
N8N_WEBHOOK_URL=http://18.191.161.60:5678/webhook/anypost/publish
BLOTATO_API_KEY=tu-blotato-api-key
BLOTATO_API_BASE_URL=https://api.blotato.com
BLOTATO_API_TEMPLATE_ID=tu-template-id
```

## Configuración en GitHub Actions

Para que los tests pasen en GitHub Actions, no necesitas configurar secrets porque el archivo `src/test/resources/application.properties` ya tiene valores dummy para todos los servicios.

Si en el futuro necesitas ejecutar tests de integración que requieran servicios reales, puedes agregar secrets en:
- Settings → Secrets and variables → Actions

Y luego usarlos en el workflow `.github/workflows/maven.yml`:

```yaml
env:
  OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
  # ... otras variables
```

## Notas Importantes

- El archivo `.env` está en `.gitignore` y **NO se subirá al repositorio**
- Si no defines las variables de entorno, la aplicación usará valores por defecto (algunos pueden estar vacíos)
- Para producción, configura estas variables en tu plataforma de despliegue (Railway, Azure, etc.)

