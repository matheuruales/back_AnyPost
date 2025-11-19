# Solución Definitiva para Error 403 en Railway

## Diagnóstico Rápido

El error 403 en `/api/auth/login` y `/api/auth/register` en Railway puede tener varias causas. Sigue estos pasos en orden:

## Paso 1: Verificar que los Cambios se Hayan Desplegado

1. Ve a [Railway Dashboard](https://railway.app)
2. Selecciona tu proyecto `backanypost-production`
3. Ve a la pestaña **"Deployments"**
4. Verifica que el último deployment tenga un estado **"Active"** y sea reciente (después de subir los cambios)
5. Si no hay un deployment reciente, haz clic en **"Redeploy"**

## Paso 2: Verificar Variables de Entorno (CRÍTICO)

El error 403 **más común** es por variables de entorno faltantes. Verifica estas variables en Railway Dashboard → Variables:

### Variables OBLIGATORIAS:

```bash
# JWT Security (MUY IMPORTANTE - sin esto, la app puede fallar)
APPLICATION_SECURITY_JWT_SECRET=<debe ser una cadena larga, mínimo 32 caracteres>
APPLICATION_SECURITY_JWT_EXPIRATION=3600000
```

> Si usas un valor más corto, el backend ahora lo hashéa para generar una clave de 256 bits y evitar el error 403, pero registra un warning. Aun así, cambia la variable por un secreto largo y aleatorio lo antes posible.

```bash

# Base de Datos
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/railway
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<tu-password-de-railway>

# Azure Blob Storage
AZURE_BLOB_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=anypost;AccountKey=...;EndpointSuffix=core.windows.net
AZURE_BLOB_CONTAINER_NAME=uploads
AZURE_BLOB_PUBLIC=true

# OpenAI
OPENAI_API_KEY=sk-proj-...
OPENAI_API_BASE_URL=https://api.openai.com/v1
OPENAI_IMAGES_MODEL=dall-e-3

# Mail
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=tu-email@gmail.com
SPRING_MAIL_PASSWORD=tu-app-password

# External Services
N8N_WEBHOOK_URL=http://18.191.161.60:5678/webhook/anypost/publish
BLOTATO_API_KEY=...
BLOTATO_API_BASE_URL=https://api.blotato.com
BLOTATO_API_TEMPLATE_ID=...
```

**⚠️ IMPORTANTE**: Si `APPLICATION_SECURITY_JWT_SECRET` no está configurado, el servicio JWT puede fallar y causar 403.

## Paso 3: Revisar Logs de Railway

1. En Railway Dashboard, ve a **"Deployments"**
2. Haz clic en el deployment más reciente
3. Revisa los logs para ver si hay errores al iniciar:
   - Busca errores de conexión a la base de datos
   - Busca errores de configuración de JWT
   - Busca errores de CORS
   - Busca cualquier excepción al iniciar

### Errores Comunes en los Logs:

```
Error: APPLICATION_SECURITY_JWT_SECRET is not configured
→ Solución: Agrega la variable APPLICATION_SECURITY_JWT_SECRET en Railway

Error: Failed to connect to database
→ Solución: Verifica SPRING_DATASOURCE_URL, USERNAME, PASSWORD

Error: Bean creation failed
→ Solución: Revisa qué bean está fallando y verifica sus dependencias
```

## Paso 4: Probar el Endpoint de Health Check

Después de desplegar, prueba este endpoint para verificar que los endpoints públicos funcionan:

```bash
curl https://backanypost-production.up.railway.app/api/auth/health
```

**Respuesta esperada:**
```json
{
  "status": "ok",
  "message": "Auth endpoint is accessible"
}
```

Si este endpoint también devuelve 403, el problema está en la configuración de seguridad.

## Paso 5: Probar Login/Register Directamente

Usa `curl` o Postman para probar directamente (sin el frontend):

```bash
# Probar Register
curl -X POST https://backanypost-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test123",
    "displayName": "Test User"
  }'

# Probar Login
curl -X POST https://backanypost-production.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test123"
  }'
```

### Si obtienes 403:

1. **Revisa los headers de respuesta** - pueden contener información sobre el error
2. **Revisa el cuerpo de la respuesta** - puede tener un mensaje de error más específico
3. **Verifica que el método HTTP sea POST** - algunos errores 403 son por usar GET en lugar de POST

## Paso 6: Verificar CORS desde el Navegador

Abre la consola del navegador (F12) y revisa:

1. **Errores de CORS antes del 403**:
   - Si ves "CORS policy" antes del 403, el problema es de CORS
   - Verifica que `app.cors.allowed-origins` esté configurado en Railway (o usa `*`)

2. **Headers de la petición**:
   - Verifica que `Content-Type: application/json` esté presente
   - Verifica que no haya headers extraños que puedan causar problemas

3. **Preflight OPTIONS**:
   - Si el navegador hace una petición OPTIONS antes del POST, verifica que responda con 200
   - Si el OPTIONS devuelve 403, el problema está en la configuración de CORS

## Soluciones Aplicadas en el Código

### 1. Corrección de CORS
- `CorsConfig.java` ahora usa correctamente `app.cors.allow-credentials`
- Maneja correctamente el caso cuando `allowCredentials=false` y se usa `"*"`

### 2. Mejora del Filtro JWT
- `JwtAuthenticationFilter.java` ahora tiene manejo de excepciones
- No bloquea peticiones a endpoints públicos si hay problemas con el JWT

### 3. Endpoint de Health Check
- Agregado `/api/auth/health` para verificar que los endpoints públicos funcionan
- Útil para diagnosticar problemas de configuración

## Si el Problema Persiste

Si después de seguir todos estos pasos el problema persiste:

1. **Verifica que el código se haya desplegado correctamente**:
   - Revisa que el commit con los cambios esté en el branch que Railway está monitoreando
   - Verifica que Railway haya detectado el push y creado un nuevo deployment

2. **Revisa la configuración de Railway**:
   - Verifica que el servicio esté configurado para usar el puerto correcto (8080)
   - Verifica que Railway esté usando el branch correcto del repositorio

3. **Prueba localmente con las mismas variables**:
   - Crea un `.env` local con las mismas variables que Railway
   - Prueba si el problema ocurre también localmente
   - Si funciona localmente pero no en Railway, el problema está en la configuración de Railway

4. **Contacta con soporte de Railway**:
   - Si nada funciona, puede ser un problema de la plataforma Railway
   - Revisa el estado de Railway en su página de estado

## Checklist Final

Antes de reportar que el problema persiste, verifica:

- [ ] Los cambios se han subido al repositorio
- [ ] Railway ha creado un nuevo deployment después de los cambios
- [ ] El deployment está en estado "Active"
- [ ] Todas las variables de entorno están configuradas en Railway
- [ ] `APPLICATION_SECURITY_JWT_SECRET` está configurado y tiene al menos 32 caracteres
- [ ] Los logs de Railway no muestran errores al iniciar
- [ ] El endpoint `/api/auth/health` responde correctamente
- [ ] La petición directa con `curl` también devuelve 403

Si todos estos puntos están verificados y el problema persiste, comparte:
1. Los logs completos de Railway del último deployment
2. La respuesta completa de `curl` (headers y cuerpo)
3. Una captura de pantalla de las variables de entorno en Railway (oculta los valores sensibles)
