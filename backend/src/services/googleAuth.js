import { OAuth2Client } from "google-auth-library";
export class GoogleAuthService {
 constructor(audience){this.audience=audience;this.client=new OAuth2Client();}
 async verify(idToken){
  try{
   const ticket=await this.client.verifyIdToken({idToken,audience:this.audience});
   const p=ticket.getPayload();
   if(!p?.sub||!p?.email||p.email_verified!==true) throw new Error("Unverified Google identity");
   return {googleSubject:p.sub,email:p.email,name:p.name||p.given_name||p.email.split("@")[0]};
  }catch{throw Object.assign(new Error("Invalid Google identity token"),{status:401});}
 }
}