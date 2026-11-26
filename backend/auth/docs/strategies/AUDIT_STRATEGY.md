# Cuaderno de auditoría

## Qué es
Un cuaderno donde la app anota quién entró, cuándo, desde dónde y qué cambió.
Sirve para responder "¿quién hizo esto?" cuando algo sale mal.

## Qué se anota

| Qué pasó | Cuándo | Qué se guarda |
|---|---|---|
| Entró | con email | quién, desde qué IP, su rol y su plan |
| Entró | con Google | lo mismo, marcado como Google |
| No pudo entrar | contraseña mal | el motivo del fallo |
| No pudo entrar | usuario inexistente | el email intentado |
| No pudo entrar | Google sin cuenta | el email de Google |
| Creó su cuenta | confirmó el email | quién y su empresa |
| Creó una empresa | primer login con Google | quién y el nombre de la empresa |
| Salió | cerró sesión | quién |
| Cambió contraseña | recuperación | quién y desde qué IP |
| Cambió un rol | lo hace el dueño | quién lo cambió, a quién y de qué a qué |
| Sacó a alguien | lo hace el dueño | quién lo sacó y a quién |

## Cómo se guarda
En la tabla `auth.audit_log`. No hace falta cambiar la base: la tabla ya admite
eventos sin empresa y un campo de detalle libre.

Los intentos fallidos se anotan en una operación separada: si el login se cancela
porque la contraseña estaba mal, la nota igual queda guardada.

## Cómo se comprueba
- Con base real: intento fallido, registro completo, login local y cambio de contraseña.
- Solo con simulaciones: cambio de rol y baja de miembro.
- Google se comprobó entrando a mano: el navegador automático es bloqueado por Google.

## Reglas
1. Si la anotación falla por un error del cuaderno, se avisa y se sigue. Si falla la base, la operación se detiene como antes de este cambio.
2. Nunca se anota contraseña, token ni datos de pago.
3. Solo se anota lo necesario: nada de montos, facturas ni clientes.
4. Por ahora no se borra nada. Si crece demasiado, se agrega limpieza por antigüedad.
   `shortcut: el cuaderno crece sin límite; agregar limpieza cuando supere ~1M filas o lo pida un cliente.`

## Qué NO entra
- Una pantalla para ver el cuaderno.
- Exportar a Excel.
- Auditar facturas, productos o clientes.
- Arreglar el error del login con Google cuando la cuenta no existe. Se anota el intento, pero el error sigue.
