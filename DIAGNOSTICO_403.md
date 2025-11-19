# Diagnóstico de Error 403 en Login/Register

## Problema
Los endpoints `/api/auth/login` y `/api/auth/register` están devolviendo **403 Forbidden** en producción (Railway).

## Posibles Causas

### 1. Variables de Entorno Faltantes en Railway (MÁS PROBABLE)

El error 403 puede ocurrir si faltan variables de entorno críticas, especialmente:

- **`APPLICATION_SECURITY_JWT_SECRET`**: Si esta variable no está configurada, el servicio JWT puede fallar y causar errores 403
- **Longitud mínima**: La clave debe tener **al menos 32 caracteres**. Si es más corta, la app ahora la hashéa para generar una llave válida y escribe un warning, pero sigue siendo obligatorio usar un valor largo y aleatorio para evitar problemas en producción.
- **`SPRING_DATASOURCE_URL`**: Si la base de datos no está configurada, la aplicación puede no iniciar correctamente
- **Otras variables críticas**: Ver `RAILWAY_SETUP.md` para la lista completa

**Solución**: Verifica en Railway Dashboard → Variables que todas las variables estén configuradas.

### 2. Problema de CORS (CORREGIDO)

**Problema anterior**: La configuración de CORS tenía `allowCredentials=true` hardcodeado, lo cual puede causar problemas cuando se usa `"*"` como origen permitido.

**Solución aplicada**: 
- Se corrigió `CorsConfig.java` para usar el valor de la propiedad `app.cors.allow-credentials` correctamente
- Cuando `allowCredentials=false`, se puede usar `"*"` como origen
- Cuando `allowCredentials=true`, se deben especificar orígenes exactos

### 3. Filtro JWT Bloqueando Peticiones (CORREGIDO)

**Problema anterior**: Si el filtro JWT lanzaba una excepción al procesar un token inválido, podía bloquear peticiones a endpoints públicos.

**Solución aplicada**:
- Se agregó manejo de excepciones en `JwtAuthenticationFilter`
- Si el procesamiento del JWT falla, se registra el error pero no se bloquea la petición
- Los endpoints públicos (`/api/auth/**`) funcionarán incluso si hay problemas con el JWT

## Pasos para Diagnosticar

### Paso 1: Verificar Variables de Entorno en Railway

1. Ve a [Railway Dashboard](https://railway.app)
2. Selecciona tu proyecto `backanypost-production`
3. Ve a la pestaña **"Variables"**
4. Verifica que estas variables estén configuradas:
   - `APPLICATION_SECURITY_JWT_SECRET` (OBLIGATORIA)
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `AZURE_BLOB_CONNECTION_STRING`
   - `OPENAI_API_KEY`
   - Y todas las demás mencionadas en `RAILWAY_SETUP.md`

### Paso 2: Revisar Logs de Railway

1. En Railway Dashboard, ve a la pestaña **"Deployments"**
2. Haz clic en el deployment más reciente
3. Revisa los logs para ver si hay errores al iniciar:
   - Errores de conexión a la base de datos
   - Errores de configuración de JWT
   - Errores de CORS

### Paso 3: Probar el Endpoint Directamente

Usa `curl` o Postman para probar el endpoint directamente:

```bash
curl -X POST https://backanypost-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test123",
    "name": "Test User"
  }'
```

Si obtienes un 403, revisa:
- Los headers de respuesta (especialmente `Access-Control-Allow-Origin`)
- El cuerpo de la respuesta (puede contener más información del error)

### Paso 4: Verificar CORS desde el Navegador

Abre la consola del navegador (F12) y revisa:
- Si hay errores de CORS antes del 403
- Los headers de la petición y respuesta
- Si el preflight OPTIONS está siendo bloqueado

## Soluciones Aplicadas

### 1. Corrección de CORS (`CorsConfig.java`)
- Ahora usa correctamente el valor de `app.cors.allow-credentials` de las propiedades
- Maneja correctamente el caso cuando `allowCredentials=false` y se usa `"*"` como origen

### 2. Mejora del Filtro JWT (`JwtAuthenticationFilter.java`)
- Agregado manejo de excepciones para no bloquear peticiones a endpoints públicos
- Agregado logging para facilitar el diagnóstico

## Próximos Pasos

1. **Sube los cambios** al repositorio
2. **Verifica que Railway redesplegue** automáticamente
3. **Revisa los logs** después del despliegue
4. **Prueba los endpoints** nuevamente

Si el problema persiste después de estos cambios:

1. Verifica que todas las variables de entorno estén configuradas en Railway
2. Revisa los logs de Railway para ver errores específicos
3. Prueba el endpoint directamente con `curl` para ver si el problema es del frontend o backend
4. Verifica que el servicio esté corriendo correctamente (status "Active" en Railway)

## Notas Adicionales

- El error 403 puede venir de Spring Security antes de llegar al controlador
- Si `APPLICATION_SECURITY_JWT_SECRET` no está configurado, el servicio JWT puede fallar al iniciar
- Los cambios en CORS y el filtro JWT deberían permitir que los endpoints públicos funcionen incluso si hay problemas con la autenticación
