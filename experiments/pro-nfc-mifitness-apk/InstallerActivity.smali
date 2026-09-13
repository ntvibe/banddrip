.class public Lorg/banddrip/mifitness/InstallerActivity;
.super Landroid/app/Activity;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 4
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
    :try_start
    new-instance v0, Lcom/xiaomi/fitness/baseui/common/FragmentParams$b;
    invoke-direct {v0}, Lcom/xiaomi/fitness/baseui/common/FragmentParams$b;-><init>()V
    const-class v1, Lcom/xiaomi/xms/wearable/ui/debug/ThirdAppDebugFragment;
    invoke-virtual {v0, v1}, Lcom/xiaomi/fitness/baseui/common/FragmentParams$b;->e(Ljava/lang/Class;)Lcom/xiaomi/fitness/baseui/common/FragmentParams$b;
    move-result-object v0
    invoke-virtual {v0}, Lcom/xiaomi/fitness/baseui/common/FragmentParams$b;->b()Lcom/xiaomi/fitness/baseui/common/FragmentParams;
    move-result-object v0
    new-instance v1, Landroid/content/Intent;
    const-class v2, Lcom/xiaomi/fitness/baseui/common/CommonBaseActivity;
    invoke-direct {v1, p0, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    const-string v2, "fragment_param"
    invoke-virtual {v1, v2, v0}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Landroid/os/Parcelable;)Landroid/content/Intent;
    invoke-virtual {p0, v1}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :failed
    return-void
    :failed
    move-exception v0
    new-instance v1, Landroid/widget/TextView;
    invoke-direct {v1, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    new-instance v2, Ljava/lang/StringBuilder;
    const-string v3, "BandDrip installer could not open. Open Mi Fitness and finish setup first, then try again.\n\nDiagnostic: "
    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V
    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-virtual {v1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const/16 v0, 24
    invoke-virtual {v1, v0, v0, v0, v0}, Landroid/widget/TextView;->setPadding(IIII)V
    invoke-virtual {p0, v1}, Landroid/app/Activity;->setContentView(Landroid/view/View;)V
    return-void
.end method
