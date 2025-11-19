# Verificación de Deployment en Railway

## ⚠️ PROBLEMA CRÍTICO

El endpoint `/api/auth/health` también está devolviendo **403 Forbidden**. Esto confirma que:

1. **Los cambios aún NO se han desplegado en Railway**, O
2. **Hay un problema más fundamental con la configuración de seguridad**

## Pasos Inmediatos para Resolver

### Paso 1: Verificar que los Cambios se Hayan Subido al Repositorio

1. Ve a tu repositorio en GitHub/GitLab
2. Verifica que el último commit incluya los cambios recientes:
   - Cambios en `SecurityConfig.java`
   - Cambios en `JwtAuthenticationFilter.java`
   - Cambios en `CorsConfig.java`
   - Cambios en `AuthController.java` (endpoint `/health`)
3. Si los cambios NO están en el repositorio:
   ```bash
   git add .
   git commit -m "Fix 403 error: improve security config and add health endpoint"
   git push origin main  # o el branch que uses
   ```

### Paso 2: Verificar Deployment en Railway

1. Ve a [Railway Dashboard](https://railway.app)
2. Selecciona tu proyecto `backanypost-production`
3. Ve a la pestaña **"Deployments"**
4. **VERIFICA**:
   - ¿Hay un deployment reciente (después de subir los cambios)?
   - ¿El deployment está en estado **"Active"**?
   - ¿El deployment tiene el commit correcto?

### Paso 3: Forzar un Nuevo Deployment

Si no hay un deployment reciente o el deployment no está activo:

1. En Railway Dashboard → Deployments
2. Haz clic en **"Redeploy"** o **"Deploy"**
3. Espera a que el deployment termine
4. Verifica que el estado sea **"Active"**

### Paso 4: Revisar Logs del Deployment

1. En Railway Dashboard → Deployments
2. Haz clic en el deployment más reciente
3. Revisa los logs para ver:
   - ¿La aplicación inició correctamente?
   - ¿Hay errores al iniciar?
   - ¿Hay mensajes sobre Spring Security?

**Busca estos errores comunes:**

```
Error: APPLICATION_SECURITY_JWT_SECRET is not configured
→ Solución: Agrega APPLICATION_SECURITY_JWT_SECRET en Railway Variables

Error: Failed to connect to database
→ Solución: Verifica SPRING_DATASOURCE_URL, USERNAME, PASSWORD

Error: Bean creation failed
→ Solución: Revisa qué bean está fallando

Error: Application failed to start
→ Solución: Revisa el error completo en los logs
```

### Paso 5: Verificar Variables de Entorno (CRÍTICO)

Incluso si los cambios se desplegaron, si faltan variables de entorno, la aplicación puede no funcionar correctamente.

1. En Railway Dashboard → Variables
2. **VERIFICA que estas variables estén configuradas:**

```bash
# OBLIGATORIAS para que la app funcione:
APPLICATION_SECURITY_JWT_SECRET=<debe tener al menos 32 caracteres>
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/railway
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<tu-password>

# Otras importantes:
AZURE_BLOB_CONNECTION_STRING=...
OPENAI_API_KEY=...
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
```

**⚠️ Si `APPLICATION_SECURITY_JWT_SECRET` está vacío o no existe, la aplicación puede fallar al iniciar o comportarse de manera inesperada.**

### Paso 6: Probar Después del Deployment

Después de verificar que el deployment está activo:

1. **Espera 1-2 minutos** para que Railway complete el deployment
2. Prueba el endpoint de health check:
   ```bash
   curl https://backanypost-production.up.railway.app/api/auth/health
   ```
3. **Respuesta esperada:**
   ```json
   {
     "status": "ok",
     "message": "Auth endpoint is accessible"
   }
   ```

Si aún devuelve 403, continúa con el Paso 7.

### Paso 7: Verificar Configuración de Railway

1. En Railway Dashboard → Settings
2. Verifica:
   - **Branch**: ¿Está configurado para usar el branch correcto? (main/master)
   - **Root Directory**: ¿Está configurado correctamente?
   - **Build Command**: ¿Está configurado correctamente? (debería ser `./mvnw clean package` o similar)
   - **Start Command**: ¿Está configurado correctamente? (debería ser `java -jar target/*.jar` o similar)

### Paso 8: Verificar que el Código se Compiló Correctamente

1. En Railway Dashboard → Deployments
2. Revisa los logs del build:
   - ¿El build fue exitoso?
   - ¿Hay errores de compilación?
   - ¿Se creó el JAR correctamente?

## Si Nada Funciona

Si después de seguir todos estos pasos el problema persiste:

1. **Verifica que el código funcione localmente:**
   ```bash
   # Localmente, con las mismas variables de entorno
   ./mvnw spring-boot:run
   # Luego prueba:
   curl http://localhost:8080/api/auth/health
   ```

2. **Si funciona localmente pero no en Railway:**
   - El problema está en la configuración de Railway
   - Revisa los logs de Railway más detalladamente
   - Considera contactar con soporte de Railway

3. **Si no funciona ni localmente:**
   - El problema está en el código
   - Revisa los logs locales para ver qué está fallando
   - Verifica que todas las variables de entorno estén configuradas localmente

## Checklist Final

Antes de reportar que el problema persiste, verifica:

- [ ] Los cambios están en el repositorio (GitHub/GitLab)
- [ ] Railway ha detectado el push y creado un nuevo deployment
- [ ] El deployment está en estado "Active"
- [ ] Los logs del deployment no muestran errores
- [ ] Todas las variables de entorno están configuradas en Railway
- [ ] `APPLICATION_SECURITY_JWT_SECRET` tiene al menos 32 caracteres
- [ ] El build fue exitoso (sin errores de compilación)
- [ ] Has esperado 1-2 minutos después del deployment
- [ ] Has probado el endpoint `/api/auth/health` después del deployment

## Información para Debugging

Si necesitas ayuda adicional, comparte:

1. **Screenshot de Railway Dashboard → Deployments** (muestra el último deployment)
2. **Logs completos del último deployment** (especialmente los últimos 50-100 líneas)
3. **Screenshot de Railway Dashboard → Variables** (oculta los valores sensibles)
4. **Resultado de `curl https://backanypost-production.up.railway.app/api/auth/health`** (completo, con headers)

