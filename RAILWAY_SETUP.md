# Configuración de Variables de Entorno en Railway

## Problema: Error 403 en Producción

Si estás recibiendo error 403 en los endpoints de login/register, es porque **faltan las variables de entorno en Railway**.

## Solución: Configurar Variables en Railway

### Paso 1: Acceder a Railway Dashboard
1. Ve a [Railway Dashboard](https://railway.app)
2. Selecciona tu proyecto `backanypost-production`
3. Ve a la pestaña **"Variables"**

### Paso 2: Agregar Variables de Entorno

Agrega las siguientes variables de entorno **OBLIGATORIAS**:

#### Base de Datos (Railway las proporciona automáticamente)
```
SPRING_DATASOURCE_URL=<Railway te da esta URL automáticamente>
SPRING_DATASOURCE_USERNAME=<Railway te da este usuario automáticamente>
SPRING_DATASOURCE_PASSWORD=<Railway te da esta contraseña automáticamente>
```

**Nota:** Railway normalmente crea estas variables automáticamente cuando conectas PostgreSQL. Si no las ves, ve a tu servicio PostgreSQL y copia la conexión.

#### Azure Blob Storage
```
AZURE_BLOB_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=anypost;AccountKey=TU-ACCOUNT-KEY-AQUI;EndpointSuffix=core.windows.net
AZURE_BLOB_CONTAINER_NAME=uploads
AZURE_BLOB_PUBLIC=true
```

#### OpenAI API
```
OPENAI_API_KEY=sk-proj-TU-OPENAI-API-KEY-AQUI
OPENAI_API_BASE_URL=https://api.openai.com/v1
OPENAI_IMAGES_MODEL=dall-e-3
```

#### JWT Security (MUY IMPORTANTE)
```
APPLICATION_SECURITY_JWT_SECRET=tu-very-long-secret-key-for-jwt-token-generation-minimo-32-caracteres
APPLICATION_SECURITY_JWT_EXPIRATION=3600000
```

#### Mail Configuration
```
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=anypost2b2t@gmail.com
SPRING_MAIL_PASSWORD=tu-app-password-de-gmail
```

#### External Services
```
N8N_WEBHOOK_URL=http://18.191.161.60:5678/webhook/anypost/publish
BLOTATO_API_KEY=tu-blotato-api-key
BLOTATO_API_BASE_URL=https://api.blotato.com
BLOTATO_API_TEMPLATE_ID=tu-template-id
```

### Paso 3: Reiniciar el Servicio

Después de agregar las variables:
1. Ve a la pestaña **"Deployments"**
2. Haz clic en **"Redeploy"** o espera a que Railway detecte los cambios y redespiegue automáticamente

## Verificación

Después de configurar las variables y redesplegar, verifica que:
1. El servicio esté corriendo (status "Active")
2. Los logs no muestren errores de conexión a la base de datos
3. Los endpoints `/api/auth/login` y `/api/auth/register` respondan correctamente

## Notas Importantes

- **NUNCA** subas el archivo `.env` al repositorio
- Railway usa las variables de entorno que configures en el dashboard
- Si cambias variables, Railway redespelgará automáticamente
- El JWT secret debe ser el mismo en todos los ambientes si quieres que los tokens funcionen entre ellos

